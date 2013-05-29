/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie.player.data;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import javax.vecmath.Color4f;
import javax.vecmath.Point3f;

/**
 * Gephi Player Binary data.<br/>
 * @author abe
 */
public class GephiBinaryFrame implements Serializable {
    private static final long serialVersionUID = -6391335967832497886L;

    public static GephiBinaryFrame my = null;

    /** 現在日時。*/
    public Date cur;
    /** ノード数。*/
    public int nodeCnt;
    /** エッジ数。*/
    public int edgeCnt;
    /** ID。*/
    public int strIdsSize;
    public String[] strIds;
    /** ラベル。*/
    public int strLabelsSize;
    public String[] strLabels;
    /** ノード頂点。*/
    public int nodesSize;
    public Point3f[] nodes;
    /** ノード頂点カラー。*/
    public Color4f[] nodesColor;
    /** エッジ頂点リスト。*/
    public int edgesSize;
    public Point3f[] edges;
    /** エッジ頂点カラー。*/
    public Color4f[] edgesColor;
    /** 矢印頂点。*/
    public int arrowsSize;
    public Point3f[] arrows;
    /** 矢印頂点カラー。*/
    public Color4f[] arrowsColor;

    public GephiBinaryFrame(int nodeMaxCnt, int edgeMaxCnt, int arrowMaxCnt) {
        nodeCnt = 0;
        edgeCnt = 0;
        strIds = new String[nodeMaxCnt];
        strLabels = new String[nodeMaxCnt];
        nodes = new Point3f[nodeMaxCnt];
        nodesColor = new Color4f[nodeMaxCnt];
        for(int i=0; i<nodeMaxCnt; i++) {
            nodes[i] = new Point3f();
            nodesColor[i] = new Color4f();
        }
        edges = new Point3f[edgeMaxCnt];
        edgesColor = new Color4f[edgeMaxCnt];
        for(int i=0; i<edgeMaxCnt; i++) {
            edges[i] = new Point3f();
            edgesColor[i] = new Color4f();
        }
        arrows = new Point3f[arrowMaxCnt];
        arrowsColor = new Color4f[arrowMaxCnt];
        for(int i=0; i<arrowMaxCnt; i++) {
            arrows[i] = new Point3f();
            arrowsColor[i] = new Color4f();
        }
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(cur);
        out.writeInt(nodeCnt);
        out.writeInt(edgeCnt);
        out.writeInt(strIds.length);
        for(String strId : strIds) {
            out.writeObject(strId);
        }
        out.writeInt(strLabels.length);
        for(String strLabel : strLabels) {
            out.writeObject(strLabel);
        }
        out.writeInt(nodes.length);
        for(int i=0; i<nodes.length; i++) {
            Point3f node = nodes[i];
            out.writeFloat(node.x);
            out.writeFloat(node.y);
            out.writeFloat(node.z);
            Color4f col = nodesColor[i];
            out.writeFloat(col.x);
            out.writeFloat(col.y);
            out.writeFloat(col.z);
            out.writeFloat(col.w);
        }
        out.writeInt(edges.length);
        for(int i=0; i<edges.length; i++) {
            Point3f edge = edges[i];
            out.writeFloat(edge.x);
            out.writeFloat(edge.y);
            out.writeFloat(edge.z);
            Color4f col = edgesColor[i];
            out.writeFloat(col.x);
            out.writeFloat(col.y);
            out.writeFloat(col.z);
            out.writeFloat(col.w);
        }
        out.writeInt(arrows.length);
        for(int i=0; i<arrows.length; i++) {
            Point3f arrow = arrows[i];
            out.writeFloat(arrow.x);
            out.writeFloat(arrow.y);
            out.writeFloat(arrow.z);
            Color4f col = arrowsColor[i];
            out.writeFloat(col.x);
            out.writeFloat(col.y);
            out.writeFloat(col.z);
            out.writeFloat(col.w);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        if (my == null) {
            cur = (Date)in.readObject();
            nodeCnt = in.readInt();
            edgeCnt = in.readInt();
            int idSize = in.readInt();
            strIdsSize = idSize;
            strIds = new String[idSize];
            for(int i=0; i<idSize; i++) {
                strIds[i] = (String)in.readObject();
            }
            int labelSize = in.readInt();
            strLabelsSize = labelSize;
            strLabels = new String[labelSize];
            for(int i=0; i<labelSize; i++) {
                strLabels[i] = (String)in.readObject();
            }
            int nodeSize = in.readInt();
            nodesSize = nodeSize;
            nodes = new Point3f[nodeSize];
            nodesColor = new Color4f[nodeSize];
            for(int i=0; i<nodeSize; i++) {
                nodes[i] = new Point3f();
                nodes[i].x = in.readFloat();
                nodes[i].y = in.readFloat();
                nodes[i].z = in.readFloat();
                nodesColor[i] = new Color4f();
                nodesColor[i].x = in.readFloat();
                nodesColor[i].y = in.readFloat();
                nodesColor[i].z = in.readFloat();
                nodesColor[i].w = in.readFloat();
            }

            int edgeSize = in.readInt();
            edgesSize = edgeSize;
            edges = new Point3f[edgeSize];
            edgesColor = new Color4f[edgeSize];
            for(int i=0; i<edgeSize; i++) {
                edges[i] = new Point3f();
                edges[i].x = in.readFloat();
                edges[i].y = in.readFloat();
                edges[i].z = in.readFloat();
                edgesColor[i] = new Color4f();
                edgesColor[i].x = in.readFloat();
                edgesColor[i].y = in.readFloat();
                edgesColor[i].z = in.readFloat();
                edgesColor[i].w = in.readFloat();
            }

            int arrowSize = in.readInt();
            arrowsSize = arrowSize;
            arrows = new Point3f[arrowSize];
            arrowsColor = new Color4f[arrowSize];
            for(int i=0; i<arrowSize; i++) {
                arrows[i] = new Point3f();
                arrows[i].x = in.readFloat();
                arrows[i].y = in.readFloat();
                arrows[i].z = in.readFloat();
                arrowsColor[i] = new Color4f();
                arrowsColor[i].x = in.readFloat();
                arrowsColor[i].y = in.readFloat();
                arrowsColor[i].z = in.readFloat();
                arrowsColor[i].w = in.readFloat();
            }
        } else {
            my.cur = (Date)in.readObject();
            my.nodeCnt = in.readInt();
            my.edgeCnt = in.readInt();
            my.strIdsSize = in.readInt();
            for(int i=0; i<my.strIdsSize; i++) {
                my.strIds[i] = (String)in.readObject();
            }
            my.strLabelsSize = in.readInt();
            for(int i=0; i<my.strLabelsSize; i++) {
                my.strLabels[i] = (String)in.readObject();
            }
            my.nodesSize = in.readInt();
            for(int i=0; i<my.nodesSize; i++) {
                my.nodes[i].x = in.readFloat();
                my.nodes[i].y = in.readFloat();
                my.nodes[i].z = in.readFloat();
                my.nodesColor[i].x = in.readFloat();
                my.nodesColor[i].y = in.readFloat();
                my.nodesColor[i].z = in.readFloat();
                my.nodesColor[i].w = in.readFloat();
            }

            my.edgesSize = in.readInt();
            for(int i=0; i<my.edgesSize; i++) {
                my.edges[i].x = in.readFloat();
                my.edges[i].y = in.readFloat();
                my.edges[i].z = in.readFloat();
                my.edgesColor[i].x = in.readFloat();
                my.edgesColor[i].y = in.readFloat();
                my.edgesColor[i].z = in.readFloat();
                my.edgesColor[i].w = in.readFloat();
            }

            my.arrowsSize = in.readInt();
            for(int i=0; i<my.arrowsSize; i++) {
                my.arrows[i].x = in.readFloat();
                my.arrows[i].y = in.readFloat();
                my.arrows[i].z = in.readFloat();
                my.arrowsColor[i].x = in.readFloat();
                my.arrowsColor[i].y = in.readFloat();
                my.arrowsColor[i].z = in.readFloat();
                my.arrowsColor[i].w = in.readFloat();
            }
        }
    }
}
