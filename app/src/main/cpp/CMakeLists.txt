cmake_minimum_required(VERSION 3.4.1)

include_directories(../../../../asio-1.12.1/include)

add_definitions(-std=c++11)
add_definitions(-DASIO_STANDALONE)

aux_source_directory(./ SRC_DIR)

add_library( chat SHARED ${SRC_DIR} )

find_library( log-lib log )

target_link_libraries( chat ${log-lib} )