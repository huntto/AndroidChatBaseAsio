#ifndef LOG_H_
#define LOG_H_

#include <android/log.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef LOG_NDEBUG
#   define DEBUG 1
#else
#   define DEBUG 0
#endif

#ifndef LOG_TAG
#define LOG_TAG     __FILE__
#endif

#define LOG_NOOP ((void)0)

#define LOG_PRINT(level, fmt, ...) \
                        __android_log_print(level, LOG_TAG, "(%s:%u) %s: " fmt, \
                                __FILE__, __LINE__, __PRETTY_FUNCTION__, ##__VA_ARGS__)

#if (DEBUG)
#define LOGV(fmt, ...) LOG_PRINT(ANDROID_LOG_VERBOSE, fmt, ##__VA_ARGS__)
#define LOGD(fmt, ...) LOG_PRINT(ANDROID_LOG_DEBUG, fmt, ##__VA_ARGS__)
#else
#define LOGV(...) LOG_NOOP
#define LOGD(...) LOG_NOOP
#endif

#define LOGI(fmt, ...) LOG_PRINT(ANDROID_LOG_INFO, fmt, ##__VA_ARGS__)
#define LOGE(fmt, ...) LOG_PRINT(ANDROID_LOG_ERROR, fmt, ##__VA_ARGS__)

#ifdef __cplusplus
}
#endif

#endif /* LOG_H_ */
