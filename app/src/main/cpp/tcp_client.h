//
// Created by xiangtao on 2018/8/2.
//

#ifndef TCP_CLIENT_H
#define TCP_CLIENT_H

#include <asio.hpp>
#include "tcp_session.h"

#if defined(LOG_TAG)
#undef LOG_TAG
#endif

#define LOG_TAG "TCPClient"

#include "log.h"

using asio::ip::tcp;

class TCPClient {
public:
    typedef std::shared_ptr<TCPClient> Pointer;
    typedef std::function<void(int, TCPSession::Pointer tcp_session, std::string)> ConnectListener;

    TCPClient(int id) : id_(id), io_context_() {
    }

    ~TCPClient() {
        Disconnect();
    }

    void Connect(const std::string ip_address, unsigned short port,
                 ConnectListener connect_listener) {
        run_thread_ = std::thread(&TCPClient::Run, this, ip_address, port, connect_listener);
    }

    void Disconnect() {
        if (!io_context_.stopped()) {
            io_context_.stop();
        }
        if (run_thread_.joinable()) {
            run_thread_.join();
        }
    }

private:
    int id_;
    asio::io_context io_context_;
    std::thread run_thread_;

    void Run(const std::string ip_address, unsigned short port,
             ConnectListener connect_listener) {
        LOGI("Run Connect:%s, %d", ip_address.c_str(), port);
        try {
            TCPSession::Pointer tcp_session = TCPSession::Create(id_, io_context_);

            tcp::endpoint endpoint(asio::ip::address_v4::from_string(ip_address), port);
            tcp::resolver resolver(io_context_);
            tcp::resolver::iterator endpoints = resolver.resolve(endpoint);
            asio::async_connect(tcp_session->Socket(), endpoints,
                                std::bind(&TCPClient::HandleConnect, this, tcp_session,
                                          connect_listener,
                                          std::placeholders::_1, std::placeholders::_2));
            io_context_.run();
        } catch (std::exception &e) {
            LOGE("Run Connect error:", e.what());
            connect_listener(id_, nullptr, e.what());
        }
    }

    void HandleConnect(TCPSession::Pointer tcp_session, ConnectListener connect_listener,
                       const asio::error_code &error,
                       tcp::resolver::iterator iter) {
        if (!error) {
            connect_listener(id_, tcp_session, "success");
        } else {
            connect_listener(id_, nullptr, error.message());
        }
    }
};

#endif //TCP_CLIENT_H