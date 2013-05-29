package org.gephi.rs.export.movie.riff.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * RIFFチャンクインターフェース。<br/>
 * RIFFチャンク階層を表現する場合は本インターフェースをインプリメントします。<br/>
 * @author abe
 */
public interface Chunk {
    public void write(OutputStream os) throws UnsupportedEncodingException, IOException;
    public String getFcc();
    public void setFcc(String fcc);
    public long getCb();
    public void setCb(long cb);
    public long getCalcCb();
}
