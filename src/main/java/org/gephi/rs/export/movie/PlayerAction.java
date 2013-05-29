/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import org.gephi.rs.export.movie.player.Player;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "Edit",
        id = "org.gephi.rs.export.movie.PlayerAction")
@ActionRegistration(
        iconBase = "org/gephi/rs/export/movie/resource/appiconIcon.png",
        displayName = "#CTL_PlayerAction")
@ActionReference(path = "Menu/Plugins", position = 3333)
@Messages("CTL_PlayerAction=GephiPlayer...")
public final class PlayerAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Player player = new Player(MovieOutputPanel.playerFilePath);
        } catch (UnsatisfiedLinkError ule) {
            JOptionPane.showMessageDialog(null, getMessage("PlayerAction.java3d-not-exist"), getMessage("PlayerAction.info"), JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private String getMessage(String resName) {
        return NbBundle.getMessage(PlayerAction.class, resName);
    }
}
