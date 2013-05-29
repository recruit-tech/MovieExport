/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JOptionPane;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.ProjectInformation;
import org.gephi.project.api.Workspace;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author gin
 */
@ActionID(category = "Edit", id = "org.gephi.rs.movie.MovieOutputAction")
@ActionRegistration(
        iconBase = "org/gephi/rs/export/movie/resource/appiconIcon.png",
        displayName = "#CTL_MovieOutputAction")
@ActionReference(path = "Menu/File", position = 1275, separatorBefore = 1237)
@Messages("CTL_MovieOutputAction=Movie Output...")
public final class MovieOutputAction implements ActionListener {
    private MovieOutputPanel movieOutputPanel;
    
    public MovieOutputAction() {
        /* 以下はJPEGコーデックの取得確認用です Batikの依存関係が不正だとwriterがnullになりトラップできないエラー原因となります
        //JPEGTranscoder t = new JPEGTranscoder();
        ImageWriter writer = ImageWriterRegistry.getInstance().getWriterFor("image/jpeg"); 
        */
        movieOutputPanel = new MovieOutputPanel();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ProjectController project = Lookup.getDefault().lookup(ProjectController.class);
        Workspace workspace = project.getCurrentWorkspace();
        
        ProjectInformation info = project.getCurrentProject().getLookup().lookup(ProjectInformation.class);
        File file = info.getFile();
        
        if (workspace == null) {
            JOptionPane.showMessageDialog(null, getMessage("MovieOutputAction.not-project-warning"), getMessage("MovieOutputAction.warning"), JOptionPane.WARNING_MESSAGE);
        } else {
            Movie movie = new Movie(workspace);
            movieOutputPanel.setFile(file);
            movieOutputPanel.setMovie(movie);
            movieOutputPanel.showDialog();
        }
    }
    
    private String getMessage(String resName) {
        return NbBundle.getMessage(MovieOutputAction.class, resName);
    }
}
