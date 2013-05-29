/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie.player.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

/**
 * Gephi Player Binary data.<br/>
 * @author abe
 */
public class GephiBinary implements Serializable {
    private static final long serialVersionUID = -8919745599597012757L;
    /** 開始日時。*/
    public Date start;
    /** 終了日時。*/
    public Date end;
    /** ノード数最大。*/
    public int nodeMaxCnt;
    /** エッジ数最大。*/
    public int edgeMaxCnt;
    /** 矢印数最大。*/
    public int arrowMaxCnt;
    /** 総枚数。*/
    public int frameCnt;
    /** 子ファイルリスト。*/
    public String[] files;

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(start);
        out.writeObject(end);
        out.writeInt(nodeMaxCnt);
        out.writeInt(edgeMaxCnt);
        out.writeInt(arrowMaxCnt);
        out.writeInt(frameCnt);
        out.writeInt(files.length);
        for(String file : files) {
            out.writeObject(file);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        start = (Date)in.readObject();
        end = (Date)in.readObject();
        nodeMaxCnt = in.readInt();
        edgeMaxCnt = in.readInt();
        arrowMaxCnt = in.readInt();
        frameCnt = in.readInt();
        int nSize = in.readInt();
        files = new String[nSize];
        for(int i=0; i<nSize; i++) {
            files[i] = (String)in.readObject();
        }
    }
}
