cmake_minimum_required(VERSION 3.22.1)

project(redriver2_android LANGUAGES C CXX)

set(REDRIVER2_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../..")
set(SRC_REBUILD "${REDRIVER2_ROOT}/src_rebuild")

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

set(SDL2_DISABLE_INSTALL ON CACHE BOOL "" FORCE)
set(SDL2_DISABLE_TESTS ON CACHE BOOL "" FORCE)
set(SDL_SHARED ON CACHE BOOL "" FORCE)
set(SDL_STATIC OFF CACHE BOOL "" FORCE)
add_subdirectory("${REDRIVER2_ROOT}/android/third_party/SDL" "${CMAKE_BINARY_DIR}/SDL2")

set(ALSOFT_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_TESTS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_CONFIG OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_HRTF_DATA OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_AMBDEC_PRESETS OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_EXAMPLES OFF CACHE BOOL "" FORCE)
set(ALSOFT_INSTALL_UTILS OFF CACHE BOOL "" FORCE)
set(ALSOFT_UPDATE_BUILD_VERSION OFF CACHE BOOL "" FORCE)
set(ALSOFT_ENABLE_MODULES OFF CACHE BOOL "" FORCE)
set(LIBTYPE SHARED CACHE STRING "" FORCE)
add_subdirectory("${REDRIVER2_ROOT}/android/third_party/openal-soft" "${CMAKE_BINARY_DIR}/openal-soft")

file(GLOB_RECURSE PSYCROSS_C CONFIGURE_DEPENDS
    "${SRC_REBUILD}/PsyCross/*.c"
)
file(GLOB_RECURSE PSYCROSS_CPP CONFIGURE_DEPENDS
    "${SRC_REBUILD}/PsyCross/*.cpp"
    "${SRC_REBUILD}/PsyCross/*.C"
    "${SRC_REBUILD}/PsyCross/*.CPP"
)

add_library(psycross STATIC ${PSYCROSS_C} ${PSYCROSS_CPP})
target_include_directories(psycross
    PUBLIC
        "${SRC_REBUILD}/PsyCross/include"
        "${SRC_REBUILD}/PsyCross/include/psx"
    PRIVATE
        "${REDRIVER2_ROOT}/android/third_party/openal-soft/include"
)
set_target_properties(psycross PROPERTIES CXX_STANDARD 11 CXX_STANDARD_REQUIRED ON)
target_compile_options(psycross PRIVATE
    -fpermissive
    -Wno-c++11-narrowing
    -Wno-narrowing
    -Wno-format
    -Wno-unused-value
)
target_compile_definitions(psycross PUBLIC NTSC_VERSION)
target_link_libraries(psycross PUBLIC SDL2::SDL2 OpenAL)

file(GLOB_RECURSE GAME_C CONFIGURE_DEPENDS
    "${SRC_REBUILD}/Game/*.c"
)
file(GLOB_RECURSE UTIL_C CONFIGURE_DEPENDS
    "${SRC_REBUILD}/utils/*.c"
)
file(GLOB_RECURSE UTIL_CPP CONFIGURE_DEPENDS
    "${SRC_REBUILD}/utils/*.cpp"
)
list(FILTER UTIL_CPP EXCLUDE REGEX ".*/video_source/.*")

add_library(main SHARED
    "${SRC_REBUILD}/redriver2_psxpc.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/android_fmv_stub.cpp"
    "${CMAKE_CURRENT_LIST_DIR}/android_sdl_main.cpp"
    ${GAME_C}
    ${UTIL_C}
    ${UTIL_CPP}
)

set_source_files_properties(${GAME_C} PROPERTIES LANGUAGE CXX)
set_target_properties(main PROPERTIES CXX_STANDARD 11 CXX_STANDARD_REQUIRED ON)

target_include_directories(main
    PRIVATE
        "${SRC_REBUILD}"
        "${SRC_REBUILD}/Game"
        "${SRC_REBUILD}/utils"
        "${SRC_REBUILD}/PsyCross/include"
        "${SRC_REBUILD}/PsyCross/include/psx"
        "${REDRIVER2_ROOT}/android/third_party/openal-soft/include"
)
target_compile_definitions(main
    PRIVATE
        NTSC_VERSION
        BUILD_CONFIGURATION_STRING="Android"
        $<$<CONFIG:Debug>:DEBUG_OPTIONS>
)
target_compile_options(main PRIVATE
    -fpermissive
    -Wno-c++11-narrowing
    -Wno-constant-conversion
    -Wno-writable-strings
    -Wno-unused-value
    -Wno-switch
    -Wno-shift-op-parentheses
    -Wno-parentheses
    -Wno-format
)
target_link_libraries(main
    PRIVATE
        psycross
        SDL2::SDL2
        OpenAL
        GLESv3
        EGL
        android
        log
)
