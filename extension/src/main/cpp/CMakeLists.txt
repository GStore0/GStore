cmake_minimum_required(VERSION 3.22.1)

project(netflix_dont_kill C CXX)

add_library(netflix_dont_kill SHARED
        netflix_dont_kill.c
        And64InlineHook/And64InlineHook.cpp
)

target_include_directories(netflix_dont_kill PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/And64InlineHook
)

target_link_libraries(netflix_dont_kill
        android
        log
        dl
)