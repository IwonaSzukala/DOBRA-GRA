package game;

import actors.Player;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class InputHandler {

    private Invaders invaders = null;
    private Player player = null;
    public Action action;

    public InputHandler(Invaders invaders, Player player) {
        this.invaders = invaders;
        this.player = player;
    }

    public void handleInput(KeyEvent event) {
        if (action == Action.PRESS) {
            if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                if (invaders.gameOver || invaders.gameWon) {
                    invaders.game();
                }
            }

            else
                player.keyPressed(event);
        }
        else if (action == Action.RELEASE)
            player.keyReleased(event);
    }

    public enum Action {
        PRESS,
        RELEASE
    }

}
