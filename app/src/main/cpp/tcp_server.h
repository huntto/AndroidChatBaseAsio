//
// Created by xiangtao on 2018/8/1.
//

#ifndef TCP_SERVER_H
#define TCP_SERVER_H

#include <asio.hpp>
#include <thread>
#include <vector>
#include <memory>
#include <string>

#include "tcp_session.h"

#if defined(LOG_TAG)
#undef LOG_TAG
#endif

#define LOG_TAG "TCPServer"

#include "log.h"

using asio::ip::tcp;

class TCPServer {

public:
    typedef std::shared_ptr<TCPServer> Pointer;
    typedef std::function<void(int, TCPSession::Pointer tcp_session, std::string)> AcceptListener;

    TCPServer(int id) : id_(id), io_context_(),
                        acceptor_(io_context_, tcp::endpoint(tcp::v4(), 0)) {
    }

    ~TCPServer() {
        StopAccept();
    }

    void StartAccept(AcceptListener accept_listener) {
        run_thread_ = std::thread(&TCPServer::Run, this, accept_listener);
    }

    void StopAccept() {
        if (!io_context_.stopped()) {
            io_context_.stop();
        }
        if (run_thread_.joinable()) {
            run_thread_.join();
        }
    }

    int GetPort() {
        return acceptor_.local_endpoint().port();
    }

    std::string GetIPAddress() {
        return acceptor_.local_endpoint().address().to_string();
    }

private:
    int id_;
    asio::io_context io_context_;

    tcp::acceptor acceptor_;
    std::thread run_thread_;

    void Run(AcceptListener accept_listener) {
        try {
            Accept(accept_listener);
            io_context_.run();
        }
        catch (std::exception &e) {
            LOGE("Run TCPServer error:%s", e.what());
        }
    }

    void HandleAccept(TCPSession::Pointer tcp_session,
                      AcceptListener accept_listener,
                      const asio::error_code &error) {
        if (!error) {
            accept_listener(id_, tcp_session, "success");
        } else {
            accept_listener(id_, nullptr, error.message());
        }
        Accept(accept_listener);
    }

    void Accept(AcceptListener accept_listener) {
        TCPSession::Pointer tcp_session
                = TCPSession::Create(id_, acceptor_.get_executor().context());

        acceptor_.async_accept(tcp_session->Socket(),
                               std::bind(&TCPServer::HandleAccept, this, tcp_session,
                                         accept_listener,
                                         std::placeholders::_1));
    }
};

#endif //TCP_SERVER_H
