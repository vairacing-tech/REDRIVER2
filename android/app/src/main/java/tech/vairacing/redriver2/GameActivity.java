package tech.vairacing.redriver2;

import android.os.Bundle;
import android.system.ErrnoException;
import android.system.Os;
import android.widget.Toast;

import org.libsdl.app.SDLActivity;

public class GameActivity extends SDLActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            String profilePath = GamePaths.profile(this).getAbsolutePath();
            Os.setenv("HOME", profilePath, true);
            Os.setenv("REDRIVER2_PROFILE_DIR", profilePath, true);
        } catch (ErrnoException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected String[] getLibraries() {
        return new String[] {
            "SDL2",
            "openal",
            "main"
        };
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
            "-ini",
            GamePaths.config(this).getAbsolutePath()
        };
    }
}
