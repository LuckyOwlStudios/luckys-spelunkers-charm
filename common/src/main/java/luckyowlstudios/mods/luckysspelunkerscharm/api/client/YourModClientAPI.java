package luckyowlstudios.mods.luckysspelunkerscharm.api.client;

import java.lang.reflect.InvocationTargetException;

public class YourModClientAPI {

    private static final InternalClientMethods __internalMethods;

    static {
        try {
            __internalMethods = (InternalClientMethods) Class.forName("luckyowlstudios.mods.luckysspelunkerscharm.client.InternalClientMethodsImpl").getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
