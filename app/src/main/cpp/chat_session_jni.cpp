#include <jni.h>
#include <string>
#include <android/native_window_jni.h>
#include <memory>
#include <unordered_map>

#if defined(LOG_TAG)
#undef LOG_TAG
#endif

#define LOG_TAG "ChatSessionJNI"

#include "log.h"

#include "tcp_server.h"
#include "tcp_client.h"


#define ARGS JNIEnv *env, jclass clazz
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

namespace {
const char *const kJavaChatSessionClassName = "me/huntto/chat/ChatSession";

jmethodID onSuccess_method;
jmethodID onError_method;
jmethodID onReceive_method;

jfieldID ip_field;
jfieldID port_field;
jfieldID is_server_field;
jfieldID id_field;

JNIEnv *GetEnv(JavaVM *vm) {
    assert(vm != nullptr);
    JNIEnv *env = nullptr;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return nullptr;
    }
    return env;
}

JNIEnv *GetAttachedEnv(JavaVM *vm) {
    assert(vm != nullptr);
    JNIEnv *env;
    if (vm->AttachCurrentThread(&env, NULL) != JNI_OK) {
        return nullptr;
    }
    return env;
}

class JavaChatSession : public std::enable_shared_from_this<JavaChatSession> {
public:
    typedef std::shared_ptr<JavaChatSession> Pointer;

    static void Init(JavaVM *vm) {
        JNIEnv *env = GetEnv(vm);
        InitMethodId(env);
        InitFiledId(env);
    }

    static Pointer Create(JNIEnv *env, jobject session_obj) {
        jstring ip_obj = static_cast<jstring>(env->GetObjectField(session_obj, ip_field));
        const char *ip_chars = NULL;
        ip_chars = env->GetStringUTFChars(ip_obj, 0);
        std::string ip_str(ip_chars);
        env->ReleaseStringUTFChars(ip_obj, ip_chars);

        return Pointer(new JavaChatSession(
                env->NewGlobalRef(session_obj),
                env->GetIntField(session_obj, id_field),
                ip_str,
                env->GetIntField(session_obj, port_field),
                env->GetBooleanField(session_obj, is_server_field)));

    }

    static void Release(JNIEnv *env, Pointer session) {
        env->DeleteGlobalRef(session->java_obj_);
    }

    static void UpdateJava(JNIEnv *env, Pointer session) {
        env->SetIntField(session->java_obj_, port_field, session->port_);
        jstring ip = env->NewStringUTF(session->ip_str_.c_str());
        env->SetObjectField(session->java_obj_, ip_field, ip);
    }

    static void CallOnSuccess(JavaVM *vm, Pointer session, int code, std::string msg) {
        JNIEnv *env = GetAttachedEnv(vm);
        jstring msg_str = env->NewStringUTF(msg.c_str());
        env->CallVoidMethod(session->java_obj_, onSuccess_method, code, msg_str);
    }

    static void CallOnError(JavaVM *vm, Pointer session, int code, std::string msg) {
        JNIEnv *env = GetAttachedEnv(vm);
        jstring msg_str = env->NewStringUTF(msg.c_str());
        env->CallVoidMethod(session->java_obj_, onError_method, code, msg_str);
    }

    static void CallOnReceiveMessage(JavaVM *vm, Pointer session, std::string msg) {
        JNIEnv *env = GetAttachedEnv(vm);
        jstring msg_str = env->NewStringUTF(msg.c_str());
        env->CallVoidMethod(session->java_obj_, onReceive_method, msg_str);
    }

    jint ID() { return id_; }

    jboolean AsServer() { return as_server_; }

    std::string GetIP() {
        return ip_str_;
    }

    jint GetPort() { return port_; }

    void SetPort(int port) { port_ = port; }

private:
    JavaChatSession(
            jobject java_obj,
            jint id,
            std::string ip_str,
            jint port,
            jboolean as_server) : java_obj_{java_obj}, id_{id}, ip_str_{ip_str}, port_{port},
                                  as_server_{as_server} {}

    jobject java_obj_;
    std::string ip_str_;
    jint port_;
    jboolean as_server_;
    jint id_;


    static void InitMethodId(JNIEnv *env) {
        jclass clazz = env->FindClass(kJavaChatSessionClassName);
        onSuccess_method = env->GetMethodID(clazz, "onSuccess", "(ILjava/lang/String;)V");
        onError_method = env->GetMethodID(clazz, "onError", "(ILjava/lang/String;)V");
        onReceive_method = env->GetMethodID(clazz, "onReceive", "(Ljava/lang/String;)V");
    }

    static void InitFiledId(JNIEnv *env) {
        jclass clazz = env->FindClass(kJavaChatSessionClassName);
        ip_field = env->GetFieldID(clazz, "mIP", "Ljava/lang/String;");
        port_field = env->GetFieldID(clazz, "mPort", "I");
        is_server_field = env->GetFieldID(clazz, "mAsServer", "Z");
        id_field = env->GetFieldID(clazz, "mID", "I");
    }
};
}

static std::unordered_map<jint, JavaChatSession::Pointer> java_chat_session_map;
static std::unordered_map<jint, TCPServer::Pointer> tcp_server_map;
static std::unordered_map<jint, TCPClient::Pointer> tcp_client_map;
static std::unordered_map<jint, std::vector<TCPSession::Pointer>> tcp_sessions_map;
static std::mutex tcp_sessions_map_mutex;

static JavaVM *VM;

const static int kConnected = 0x01;
const static int kConnectFailed = 0x02;

const static int kAccepted = 0x03;
const static int kAcceptFailed = 0x04;

const static int kSendMsgSuccess = 0x05;
const static int kSendMsgFiled = 0x06;

static void HandleOnGetTcpSession(int id,
                                  TCPSession::Pointer tcp_session,
                                  std::string msg, std::pair<int, int> result_codes) {
    if (java_chat_session_map.count(id) > 0) {
        if (tcp_session == nullptr) {
            JavaChatSession::CallOnError(VM, java_chat_session_map[id], result_codes.second,
                                         msg);
        } else {
            {
                std::lock_guard<std::mutex> lock(tcp_sessions_map_mutex);
                tcp_sessions_map[id].push_back(tcp_session);
            }
            JavaChatSession::CallOnSuccess(VM, java_chat_session_map[id], result_codes.first, msg);
            tcp_session->Read([](int id, std::string msg) {
                if (java_chat_session_map.count(id) > 0) {
                    JavaChatSession::Pointer chat_session = java_chat_session_map[id];
                    JavaChatSession::CallOnReceiveMessage(VM, chat_session, msg);
                }
            });
        }
    }
}

static void CreateServer(JavaChatSession::Pointer chat_session) {
    int session_id = chat_session->ID();
    TCPServer::Pointer tcp_server = TCPServer::Pointer(new TCPServer(session_id));
    chat_session->SetPort(tcp_server->GetPort());

    tcp_server->StartAccept(
            [](int id,
               TCPSession::Pointer tcp_session,
               std::string msg) {
                HandleOnGetTcpSession(id, tcp_session, msg, std::make_pair<int, int>(
                        const_cast<int &&>(kAccepted),
                        const_cast<int &&>(kAcceptFailed)));
            }
    );

    tcp_server_map[session_id] = tcp_server;
}

static void CreateClient(JavaChatSession::Pointer chat_session) {
    int session_id = chat_session->ID();
    TCPClient::Pointer tcp_client = TCPClient::Pointer(new TCPClient(session_id));

    tcp_client->Connect(chat_session->GetIP(),
                        static_cast<unsigned short>(chat_session->GetPort()),
                        [](int id, TCPSession::Pointer tcp_session,
                           std::string msg) {

                            HandleOnGetTcpSession(id, tcp_session, msg, std::make_pair<int, int>(
                                    const_cast<int &&>(kConnected),
                                    const_cast<int &&>(kConnectFailed)));
                            LOGI("client:%s", msg.c_str());
                        }
    );

    tcp_client_map[session_id] = tcp_client;
}

static jboolean NativeNewInstance(ARGS, jobject session_obj) {
    LOGD("NativeNewInstance");
    JavaChatSession::Pointer chat_session = JavaChatSession::Create(env, session_obj);

    int id = chat_session->ID();
    java_chat_session_map[id] = chat_session;

    if (chat_session->AsServer()) {
        CreateServer(chat_session);
    } else {
        CreateClient(chat_session);
    }

    JavaChatSession::UpdateJava(env, chat_session);
    return JNI_TRUE;
}

static jboolean NativeRelease(ARGS, jint id) {
    LOGD("nativeRelease");
    tcp_server_map.erase(id);
    tcp_client_map.erase(id);
    {
        std::lock_guard<std::mutex> lock(tcp_sessions_map_mutex);
        tcp_sessions_map[id].clear();
        tcp_sessions_map.erase(id);
    }

    return JNI_TRUE;
}

static jboolean NativeSendMessage(ARGS, jint id, jstring msg_obj) {
    LOGV("NativeSendMessage");
    if (java_chat_session_map.count(id) < 1) {
        LOGE("No chat session id:%d", id);
        return JNI_FALSE;
    }

    const char *msg_chars = env->GetStringUTFChars(msg_obj, 0);
    std::string msg(msg_chars);
    env->ReleaseStringUTFChars(msg_obj, msg_chars);

    std::lock_guard<std::mutex> lock(tcp_sessions_map_mutex);
    if (tcp_sessions_map.count(id) > 0) {
        for (auto tcp_session : tcp_sessions_map[id]) {
            tcp_session->Write(msg, [](int id, bool success, std::string msg) {
                JavaChatSession::Pointer chat_session = java_chat_session_map[id];
                if (success) {
                    JavaChatSession::CallOnSuccess(VM, chat_session, kSendMsgSuccess, msg);
                } else {
                    JavaChatSession::CallOnError(VM, chat_session, kSendMsgFiled, msg);
                }
            });
        }
    }

    return JNI_TRUE;
}

static int RegisterNativeMethod(JavaVM *vm) {
    JNIEnv *env = GetEnv(vm);
    JNINativeMethod methods[] = {
            {"nativeNewInstance", "(Lme/huntto/chat/ChatSession;)Z", (void *) NativeNewInstance},
            {"nativeRelease",     "(I)Z",                            (void *) NativeRelease},
            {"nativeSendMessage", "(ILjava/lang/String;)Z",          (void *) NativeSendMessage}
    };

    jclass clazz = env->FindClass(kJavaChatSessionClassName);
    return env->RegisterNatives(clazz, methods, NELEM(methods));
}


jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    VM = vm;

    JavaChatSession::Init(vm);
    if (RegisterNativeMethod(vm) < 0) {
        return -1;
    }

    return JNI_VERSION_1_6;
}