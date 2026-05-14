extern int main(int argc, char** argv);

extern "C" int SDL_main(int argc, char* argv[])
{
    return main(argc, argv);
}
