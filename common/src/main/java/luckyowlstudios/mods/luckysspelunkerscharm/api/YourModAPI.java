package luckyowlstudios.mods.luckysspelunkerscharm.api;

import java.lang.reflect.InvocationTargetException;

public class YourModAPI {
    public static final String MOD_ID = "yourmod";

    private static final InternalMethods __internalMethods;

    static {
        try {
            __internalMethods = (InternalMethods) Class.forName("luckyowlstudios.mods.luckysspelunkerscharm.InternalMethodsImpl").getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
