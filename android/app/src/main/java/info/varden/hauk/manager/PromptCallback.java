package info.varden.hauk.manager;

/**
 * A callback interface that should be passed to all user prompts. The prompt calls either {@code
 * accept()} or {@code deny()} depending on the user's response to the prompt.
 *
 * @author Marius Lindvall
 */
public interface PromptCallback {
    /**
     * Called if the user accepted the prompt.
     */
    void accept();

    /**
     * Called if the user denied the prompt.
     */
    void deny();
}
