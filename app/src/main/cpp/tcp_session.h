//
// Created by xiangtao on 2018/8/1.
//

#ifndef TCP_SESSION_H
#define TCP_SESSION_H

#include <asio.hpp>
#include <functional>
#include <thread>


class TCPSession
        : public std::enable_shared_from_this<TCPSession> {
public:
    typedef std::shared_ptr<TCPSession> Pointer;
    typedef std::function<void(int id, std::string)> ReadListener;
    typedef std::function<void(int id, bool, std::string)> WriteListener;

    static Pointer
    Create(int id, asio::io_context &io_context) {
        return Pointer(new TCPSession(id, io_context));
    }

    asio::ip::tcp::socket &Socket() {
        return socket_;
    }

    void Write(std::string message, WriteListener write_listener) {
        socket_.async_write_some(
                asio::buffer(message),
                std::bind(&TCPSession::HandleWrite, shared_from_this(),
                          message,
                          write_listener,
                          std::placeholders::_1, std::placeholders::_2));
    }

    void Read(ReadListener read_listener) {
        receive_msg_.assign(receive_msg_.size(), 0);
        socket_.async_read_some(asio::buffer(receive_msg_),
                                std::bind(&TCPSession::HandleRead, shared_from_this(),
                                          read_listener,
                                          std::placeholders::_1, std::placeholders::_2));
    }

private:
    TCPSession(int id, asio::io_context &io_context)
            : id_{id}, socket_(io_context) {
        receive_msg_.resize(2048);
    }

    int id_;
    std::vector<char> receive_msg_;

    void HandleWrite(std::string message, WriteListener write_listener,
                     const asio::error_code &error,
                     size_t /*bytes_transferred*/) {
        if (error) {
            write_listener(id_, false, message);
        } else {
            write_listener(id_, true, message);
        }
    }

    void HandleRead(ReadListener read_listener,
                    const asio::error_code &error,
                    size_t receive_bytes) {
        if (!error && receive_bytes > 0) {
            read_listener(id_, std::string(receive_msg_.data()));
        }
        Read(read_listener);
    }

    asio::ip::tcp::socket socket_;
};


#endif //TCP_SESSION_H
