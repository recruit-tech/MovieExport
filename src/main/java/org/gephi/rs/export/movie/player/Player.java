/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.J3DGraphics2D;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TriangleArray;
import javax.media.j3d.View;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.ViewingPlatform;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.gephi.rs.export.movie.MovieOutputPanel;
import org.gephi.rs.export.movie.player.data.GephiBinary;
import org.gephi.rs.export.movie.player.data.GephiBinaryFrame;
import org.openide.util.NbBundle;

/**
 * Gephiプレイヤー(GUIアプリケーション)。<br/>
 * 名前はGephiプレイヤーですが、SVGtoPlayerBinary.javaで作成したgdata.rs3と*.rs3cのファイルを順次読み込み表示するだけのプレイヤーです。<br/>
 * <b>引数でパスを指定します。</b><br/>
 * 指定したパスの中に以下のファイルが含まれていることが前提となります。<br/>
 * <table border="1">
 * <tr><td>gdata.rs3</td><td>再生用親ファイル</td></tr>
 * <tr><td>*.rs3c</td><td>フレーム毎のバイナリデータファイル</td></tr>
 * <tr><td>DL_DAU.csv</td><td>日別のDL数とDAU数を記載したCSVファイルです</td></tr>
 * <tr><td>top10.csv</td><td>日別のTweetトップ10を記載したCSVファイルです</td></tr>
 * </table>
 * <br/>
 * 引数は以下のように指定します。<br/>
 * <table border="1">
 * <tr><td>第1引数</td><td>再生用ファイルが収められているパス</td></tr>
 * <tr><td>第2引数</td><td>フレームバッファの数を指定します。省略時は25フレーム<br/></td></tr>
 * </table>
 * @author abe
 */
public class Player extends JFrame implements ActionListener, MouseListener, MouseMotionListener, MouseWheelListener, WindowListener, Runnable {

    private static final long serialVersionUID = 1L;

    private String strPath = "";

    private static GraphicsDevice device;

    /** 描画用パネル。*/
    private MainPanel mainPanel;
    
    /** メニュー。*/
    private JMenuBar menuBar;
    private JMenu menu;
    private JMenuItem[] menuItems;

    private boolean bFullscreenOK;
    private boolean bFullscreen;
    private boolean bFullscreenChange;

    private GephiBinary gb = null;

    private static int frameBufferMax = 25; // 1秒分 (25)
    private GephiBinaryFrame[] arrFrames;
    private int frameTop;
    private int frameRead;
    private int frameCur;
    public int frameMax;

    private Thread readThread;
    private boolean bStop;

    private Point posCamera;                // カメラ平行移動制御用
    private Point posRot;                   // オブジェクト回転制御用
    private double rotX;                    // 現在のX軸角度
    private double rotY;                    // 現在のY軸角度

    private final Object lock = new Object();

    private final double maxDown = 1000.0;
    private final double maxUp = 200000.0;

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("引数にパスを指定してください。");
            return;
        }
        if (args.length >= 2) {
            int bufferSize = Integer.parseInt(args[1]);
            frameBufferMax = bufferSize;
        }
        new Player(args[0]);
    }

    public Player(String path) {
        super(SimpleUniverse.getPreferredConfiguration());
        
        strPath = path;
        arrFrames = new GephiBinaryFrame[frameBufferMax];

        GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment() ;
        device = env.getDefaultScreenDevice() ;
        if (!device.isFullScreenSupported()) {
            System.out.println("Full screen not supported.");
            bFullscreenOK = false;
        } else {
            bFullscreenOK = true;
        }

        // メニュー追加
        String[] strMenus = {getMessage("Player.menuFile.open"), null, getMessage("Player.menuFile.exit")};
        int[] mnemonics = {-1, -1, KeyEvent.VK_X};
        KeyStroke[] strokes = {KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), null, null};
        menu = new JMenu(getMessage("Player.menuFile"));
        menu.setMnemonic(KeyEvent.VK_F);
        menuItems = new JMenuItem[strMenus.length];
        for(int i=0; i<strMenus.length; i++) {
            if (strMenus[i] == null) {
                menu.addSeparator();
            } else {
                menuItems[i] = new JMenuItem(strMenus[i]);
                menuItems[i].addActionListener(this);
                if (mnemonics[i] != -1)
                    menuItems[i].setMnemonic(mnemonics[i]);
                if (strokes[i] != null)
                    menuItems[i].setAccelerator(strokes[i]);
                menu.add(menuItems[i]);
            }
        }
        menuBar = new JMenuBar();
        menuBar.add(menu);
        setJMenuBar(menuBar);

        setResizable(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addWindowListener(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setTitle("GephiPlayer");
        ClassLoader cl = getClass().getClassLoader();
        ImageIcon icon = new ImageIcon(cl.getResource("org/gephi/rs/export/movie/resource/appicon.png"));
        setIconImage(icon.getImage());

        if (strPath != null) {
            open();
        } else {
            mainPanel = new MainPanel(1280, 720);
            setContentPane(mainPanel);
            pack();	// コンポーネントサイズにウインドウを変更
        }
        setVisible(true);
    }
    
    private void open() {
        if (mainPanel != null) {
            mainPanel.stop();
        }
        if (readThread != null) {
            bStop = true;
            try {
                readThread.join();
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
        }
        frameTop = 0;
        frameRead = 0;
        frameCur = 0;
        frameMax = 0;
        try {
            File f = new File(strPath);
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            gb = (GephiBinary)ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        bStop = false;
        readThread = new Thread(this);
        readThread.start();
        
        mainPanel = new MainPanel(1280, 720);
        setContentPane(mainPanel);
        pack();	// コンポーネントサイズにウインドウを変更
        mainPanel.init(this);

        bFullscreen = false;
        bFullscreenChange = false;

        posCamera = null;
        posRot = null;
        rotX = 0.0;
        rotY = 0.0;
    }

    @Override
    public void run() {
        try {
            File file = new File(strPath);
            String strParent = file.getParent() + "/";
            while (!bStop) {
                int nFrameMax;
                int nFrameCur;
                int nFrameTop;
                int nFrameRead;
                synchronized (lock) {
                    nFrameMax = frameMax + 1;
                    nFrameCur = frameCur;
                    nFrameTop = frameTop;
                    nFrameRead = frameRead;

                    if (nFrameMax >= frameBufferMax) {
                        nFrameMax = 0;
                    }
                    if ((nFrameMax != nFrameCur) && (nFrameTop + nFrameRead < gb.frameCnt)) {
                        FileInputStream fis2 = new FileInputStream(strParent + gb.files[nFrameTop + nFrameRead]);
                        ObjectInputStream ois2 = new ObjectInputStream(fis2);
                        if (arrFrames[frameMax] == null) {
                            arrFrames[frameMax] = new GephiBinaryFrame(gb.nodeMaxCnt, gb.edgeMaxCnt, gb.arrowMaxCnt);
                        }
                        GephiBinaryFrame.my = arrFrames[frameMax];
                        ois2.readObject();
                        ois2.close();
                        frameRead++;
                        frameMax = nFrameMax;
                    }
                }
                try {
                    Thread.sleep(20);
                } catch(InterruptedException e) {
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public GephiBinaryFrame getFrame() {
        GephiBinaryFrame ret = null;
        if (frameCur != frameMax) {
            ret = arrFrames[frameCur++];
            if (frameCur >= frameBufferMax) {
                frameCur = 0;
            }
        }
        return ret;
    }

    public void setSeekFrame(int nIdx) {
        synchronized (lock) {
            frameTop = nIdx;
            frameRead = 0;
            frameCur = 0;
            frameMax = 0;
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // メニュー選択
        if (e.getSource() == menuItems[0]) {
            // 開く...
            JFileChooser fc;
            if (strPath != null) {
                File file = new File(strPath);
                fc = new JFileChooser(file.getParent());
            } else {
                fc = new JFileChooser();
            }
            FileFilter filter = new FileNameExtensionFilter("RS3", "rs3", "rs3");
            fc.addChoosableFileFilter(filter);
            fc.setAcceptAllFileFilterUsed(false);
            int ret = fc.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                strPath = file.getPath();
                open();
            }
        } else if (e.getSource() == menuItems[2]) {
            // 終了 WINDOW_CLOSINGイベントをポストします。
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        }
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        
        int ret = JOptionPane.showConfirmDialog(this, getMessage("Player.exitQuestionLabel.text"), getMessage("Player.exitLabel.text"), JOptionPane.YES_NO_OPTION);
        if (ret == JOptionPane.YES_NO_OPTION) {
            mainPanel.stop();
            bStop = true;
            if (readThread != null) {
                try {
                    readThread.join();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
            dispose();	// WINDOW_CLOSEDイベントが呼び出されます
        }
    }

    @Override
    public void windowClosed(WindowEvent e) {
        if (!bFullscreenChange) {
            System.exit(0);	// アプリケーション終了
        } else {
            bFullscreenChange = !bFullscreenChange;
        }
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (e.getClickCount() >= 2){
                // ダブルクリック (ノーマル・フルスクリーン切り替え)
                if (bFullscreenOK) {
                    bFullscreen = !bFullscreen;
                    bFullscreenChange = true;
                    mainPanel.exit();
                    dispose();
                    if (bFullscreen) {
                        setJMenuBar(null);
                        setUndecorated(true);
                        device.setFullScreenWindow(this);
                    } else {
                        setJMenuBar(menuBar);
                        setUndecorated(false);
                        device.setFullScreenWindow(null);
                    }
                    mainPanel.init(this);
                    setVisible(true);
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (e.getClickCount() >= 2) {
                // ダブルクリック、表示位置リセット
                mainPanel.resetCameraPos();
                rotX = rotY = 0.0;
                mainPanel.setRotX(rotX);
                mainPanel.setRotY(rotY);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            // カメラ平行移動
            posCamera = e.getPoint();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            // オブジェクト回転制御
            posRot = e.getPoint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            if (posCamera != null) {
                // カメラ平行移動
                Point pos = e.getPoint();
                pos.x = posCamera.x - pos.x;
                pos.y = posCamera.y - pos.y;
                Vector3d vec = mainPanel.getCameraPos();
                vec.x += pos.x * (10.0 * (vec.z / 15000.0));
                vec.y -= pos.y * (10.0 * (vec.z / 15000.0));
                mainPanel.setCameraPos(vec);
                posCamera = null;
            }
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            if (posRot != null) {
                // オブジェクト回転制御 (終了)
                Point pos = e.getPoint();
                double x = ((double)pos.x - (double)posRot.x) * 0.2;
                double y = ((double)pos.y - (double)posRot.y) * 0.2;
                x = ((x > 360.0) || (x < -360.0) ? x % 360.0 : x);
                rotX += x;
                y = ((y > 360.0) || (y < -360.0) ? y % 360.0 : y);
                rotY += y;
                mainPanel.setRotX(rotY);
                mainPanel.setRotY(rotX);
                posRot = null;
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (posCamera != null) {
            Point pos = e.getPoint();
            pos.x = posCamera.x - pos.x;
            pos.y = posCamera.y - pos.y;
            Vector3d vec = mainPanel.getCameraPos();
            vec.x += pos.x * (10.0 * (vec.z / 15000.0));
            vec.y -= pos.y * (10.0 * (vec.z / 15000.0));
            mainPanel.setCameraPos(vec);
            posCamera = e.getPoint();
        }
        if (posRot != null) {
            // マウス右クリック、回転制御
            Point pos = e.getPoint();
            double x = ((double)pos.x - (double)posRot.x) * 0.2;
            double y = ((double)pos.y - (double)posRot.y) * 0.2;
            x = ((x > 360.0) || (x < -360.0) ? x % 360.0 : x);
            mainPanel.setRotY(rotX + x);
            y = ((y > 360.0) || (y < -360.0) ? y % 360.0 : y);
            mainPanel.setRotX(rotY + y);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int n = e.getWheelRotation();
        if (n != 0) {
            Vector3d vec = mainPanel.getCameraPos();
            if (n > 0) {
                if (vec.z > maxDown) {
                    vec.z -= 500.0;
                    mainPanel.setCameraPos(vec);
                }
            } else if (n < 0) {
                if (vec.z < maxUp) {
                    vec.z += 500.0;
                    mainPanel.setCameraPos(vec);
                }
            }
        }
    }

    /**
     * 文字列取得。<br/>
     * @param resName
     * @return 
     */
    private String getMessage(String resName) {
        return NbBundle.getMessage(MovieOutputPanel.class, resName);
    }

    /**
     * 描画領域用のパネル。<br/>
     * @author gin
     */
    class MainPanel extends JLayeredPane implements ComponentListener, ChangeListener, MouseListener, MouseMotionListener, Runnable {
        private static final long serialVersionUID = 1L;

        private Player main = null;

        /** 推奨サイズ。*/
        private Dimension viewSize = new Dimension();

        /** Java3D canvas。*/
        private Canvas3D canvas = null;
        private SimpleUniverse universe = null;

        private Vector3d vecCamera = null;
        private Appearance attrEdge = null;
        private Appearance attrArrow = null;
        private Appearance attrNode = null;
        private double rotX;	// X軸回転角度
        private double rotY;	// Y軸回転角度

        private Thread backThread = null;
        private boolean bStop = false;

        /** 現在の読み込み位置。*/
        private int readCnt;

        /** 2D描画用イメージ。*/
        private Image imgGraph;
        /** 日付、ノード数、エッジ数の描画トップ座標。*/
        private Point posDateNodeEdge;

        // 操作系
        private JPanel controlPanel;
        private JSlider seekBar;
        private int nSeekDrag;
        private JButton playBtn;
        private boolean bPlay;
        private boolean bWait;
        private JSlider updownBar;
        private ImageIcon[] btnIcon;

        private final Object lock = new Object();

        /** ATLロゴイメージ。*/
        private Image imgATLLogo;

        /**
         * コンストラクタ。<br/>
         * 推奨サイズの保存。<br/>
         * @param width 幅
         * @param height 高さ
         */
        public MainPanel(int width, int height) {
            viewSize.setSize(width, height);

            addComponentListener(this);
            readCnt = 0;
            bStop = false;
            vecCamera = new Vector3d(0.0, 0.0, 15000.0);
            rotX = 0.0;
            rotY = 0.0;

            bPlay = false;
            bWait = false;

            btnIcon = new ImageIcon[2];
            ClassLoader cl = getClass().getClassLoader();
            btnIcon[0] = new ImageIcon(cl.getResource("org/gephi/rs/export/movie/resource/play.png"));
            btnIcon[1] = new ImageIcon(cl.getResource("org/gephi/rs/export/movie/resource/stop.png"));

            // ATLロゴ読み込み
            ImageIcon iiATL = new ImageIcon(cl.getResource("org/gephi/rs/export/movie/resource/ATL_logo_mini.png"));
            int atlW = iiATL.getIconWidth();
            int atlW2 = atlW;
            int atlH = iiATL.getIconHeight();
            int atlH2 = atlH;
            imgATLLogo = new BufferedImage(atlW2, atlH2, BufferedImage.TYPE_INT_ARGB);
            Graphics g = imgATLLogo.getGraphics();
            g.drawImage(iiATL.getImage(), 0, 0, atlW2-1, atlH2-1, 0, 0, atlW-1, atlH-1, null);
        }

        /**
         * 親のpack()時の推奨サイズ。<br/>
         */
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(viewSize.width, viewSize.height);
        }

        /**
         * 描画初期化。<br/>
         */
        public void init(Player parent) {
            boolean bFirst = false;
            if (main == null)
                bFirst = true;
            main = parent;

            if (bFirst) {
                setLayout(null);

                // 操作用パネル
                controlPanel = new JPanel();
                controlPanel.setLayout(null);
                controlPanel.setBackground(new Color(0, 0, 0));
                playBtn = new JButton(btnIcon[0]);
                playBtn.addMouseListener(this);
                seekBar = new JSlider(0, gb.frameCnt, 0);
                seekBar.setBackground(new Color(0, 0, 0));
                seekBar.addChangeListener(this);
                seekBar.addMouseListener(this);
                seekBar.addMouseMotionListener(this);
                updownBar = new JSlider(JSlider.VERTICAL, (int)maxDown, (int)maxUp, (int)vecCamera.z);
                updownBar.setBackground(new Color(0, 0, 0));
                updownBar.addChangeListener(this);
                updownBar.addMouseListener(this);
                updownBar.addMouseMotionListener(this);
                controlPanel.add(playBtn);
                controlPanel.add(seekBar);
                controlPanel.setBounds(0, viewSize.height-50, viewSize.width, 50);
                playBtn.setBounds(10, 10, 50, controlPanel.getSize().height-20);
                seekBar.setBounds(70, 0, controlPanel.getSize().width - 70, controlPanel.getSize().height);
                int gh = (int)(viewSize.height * 0.23) + 10;
                updownBar.setBounds(viewSize.width - 20, gh, 20, viewSize.height - controlPanel.getSize().height - gh);
                add(controlPanel);
                add(updownBar);
            }

            // Java3D初期化
            GraphicsConfiguration config = SimpleUniverse.getPreferredConfiguration();
            canvas = new Canvas3D(config);
            canvas.setBounds(0, 0, viewSize.width, viewSize.height);
            canvas.addMouseListener(parent);
            canvas.addMouseMotionListener(parent);
            canvas.stopRenderer();
            add(canvas);

            // 空間生成
            universe = new SimpleUniverse(canvas);

            universe.getViewer().getView().setBackClipPolicy(View.VIRTUAL_EYE);
            universe.getViewer().getView().setBackClipDistance(maxUp);

            // 視点設定
            setCameraPos(vecCamera);

            // Edge属性設定
            LineAttributes lattr = new LineAttributes();
            lattr.setLineWidth(0.2f);                                   // 線の太さ
            lattr.setLinePattern(LineAttributes.PATTERN_SOLID);         // 実線
            lattr.setLineAntialiasingEnable(true);			// アンチエイリアス
            attrEdge = new Appearance();
            attrEdge.setLineAttributes(lattr);

            // Arrow属性設定
            PolygonAttributes pattr = new PolygonAttributes();
            pattr.setPolygonMode(PolygonAttributes.POLYGON_FILL);	// 面
            pattr.setCullFace(PolygonAttributes.CULL_BACK);		// 裏面は描画なし
            attrArrow = new Appearance();
            attrArrow.setPolygonAttributes(pattr);

            // Node属性設定
            PolygonAttributes pattr2 = new PolygonAttributes();
            pattr2.setPolygonMode(PolygonAttributes.POLYGON_FILL);	// 面
            pattr2.setCullFace(PolygonAttributes.CULL_BACK);		// 裏面は描画なし
            attrNode = new Appearance();
            attrNode.setPolygonAttributes(pattr2);

            create2DImage(viewSize);

            bStop = false;
            backThread = new Thread(this);
            backThread.start();
        }

        public void create2DImage(Dimension size) {
            SimpleDateFormat sdfl = new SimpleDateFormat(getMessage("Movie.DatetimeFormat"));
            imgGraph = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);

            Graphics graphics = imgGraph.getGraphics();

            // 日付、ノード数、エッジ数
            int y = 10;
            String strCur = String.format("yyyy/mm/dd hh:mm:ss (%s ～ %s)", sdfl.format(gb.start), sdfl.format(gb.end));
            graphics.setFont(fontNodeAndEdge);
            FontMetrics fm = graphics.getFontMetrics();
            graphics.setColor(new Color(32, 32, 32, 200));
            int w = fm.stringWidth(strCur) + 10;
            int h = (fm.getHeight()+5) * 2 + 5;
            posDateNodeEdge = new Point(size.width - w - 5 - updownBar.getSize().width, size.height - h - y - seekBar.getSize().height);
            graphics.fillRect(posDateNodeEdge.x, posDateNodeEdge.y, w, h);
        }

        public void exit() {
            stop();
            if (canvas != null) {
                remove(canvas);
                canvas = null;
            }
        }

        public Vector3d getCameraPos() {
            return vecCamera;
        }

        public void setCameraPos(Vector3d vec) {
            vecCamera = vec;
            ViewingPlatform camera = universe.getViewingPlatform();
            TransformGroup camPos = camera.getViewPlatformTransform();
            Transform3D t = new Transform3D();
            t.setTranslation(vecCamera);
            camPos.setTransform(t);
            updownBar.setValue((int)vecCamera.z);
        }

        // カメラ位置リセット
        public void resetCameraPos() {
            vecCamera = new Vector3d(0.0, 0.0, 15000.0);
            setCameraPos(vecCamera);
        }

        /**
         * オブジェクトX軸回転
         * @param x 角度
         */
        public void setRotX(double x) {
            rotX = x;
        }

        /**
         * オブジェクトY軸回転
         * @param y 角度
         */
        public void setRotY(double y) {
            rotY = y;
        }

        private GephiBinaryFrame gbf = null;
        private static final double toRAD = Math.PI / 180.0;

        private boolean arrayUpdate = false;
        private Shape3D edgesShape3D;
        private Shape3D arrowShape3D;
        private Shape3D nodesAhspe3D;
        private TriangleArray nodesArr;

        // 描画用 (renderが呼ばれる度にnewされないように使いまわしよう
        private SimpleDateFormat sdf = new SimpleDateFormat(getMessage("Movie.DatetimeFormat"));
        private SimpleDateFormat sdf2 = new SimpleDateFormat(getMessage("Movie.DateFormat"));
        private Appearance appearance = new Appearance();
        private Font fontLabel = new Font("MS UI Gothic", Font.BOLD, 12);
        private Color colLabel = new Color(128, 128, 128, 200);
        private Transform3D temp = new Transform3D();
        private Point2d posScreen = new Point2d();
        private Font fontTop = new Font("MS UI Gothic", Font.PLAIN, 10);
        private Font fontNodeAndEdge = new Font("MS UI Gothic", Font.PLAIN, 12);
        private Color colNodeAndEdge = new Color(192, 192, 192);
        private List<String> arrNodeAndEdgeStr = new ArrayList<String>();
        private Transform3D t3dx = new Transform3D();
        private Transform3D t3dy = new Transform3D();
        private Point3d posWorld = new Point3d();

        public void render() {
            synchronized (lock) {
                if ((bPlay && !bWait) || (gbf == null)) {
                    GephiBinaryFrame lgbf = main.getFrame();
                    if (lgbf != null) {
                        gbf = lgbf;
                        seekBar.setValue(readCnt);
                        readCnt++;
                        arrayUpdate = true;
                    }
                    if (gbf == null)
                        return;
                }
                GraphicsContext3D gc = canvas.getGraphicsContext3D();

                gc.clear();
                gc.setAppearance(appearance);

                // Edge設定
                if (gbf.edgesSize > 0) {
                    if (arrayUpdate) {
                        LineArray edgesArr = new LineArray(gbf.edgesSize, GeometryArray.COORDINATES | GeometryArray.COLOR_4);
                        edgesArr.setCoordinates(0, gbf.edges, 0, gbf.edgesSize);
                        edgesArr.setColors(0, gbf.edgesColor, 0, gbf.edgesSize);
                        edgesShape3D = new Shape3D(edgesArr, attrEdge);
                    }
                    gc.draw(edgesShape3D);
                }

                // Arraw設定
                if (gbf.arrowsSize > 0) {
                    if (arrayUpdate) {
                        TriangleArray arrowArr = new TriangleArray(gbf.arrowsSize, GeometryArray.COORDINATES | GeometryArray.COLOR_4);
                        arrowArr.setCoordinates(0, gbf.arrows, 0, gbf.arrowsSize);
                        arrowArr.setColors(0, gbf.arrowsColor, 0, gbf.arrowsSize);
                        arrowShape3D = new Shape3D(arrowArr, attrArrow);
                    }
                    gc.draw(arrowShape3D);
                }

                // Node設定
                if (gbf.nodesSize > 0) {
                    if (arrayUpdate) {
                        nodesArr = new TriangleArray(gbf.nodesSize, GeometryArray.COORDINATES | GeometryArray.COLOR_4);
                        //QuadArray nodesArr = new QuadArray(gbf.nodes.length, GeometryArray.COORDINATES | GeometryArray.COLOR_4);
                        nodesArr.setCoordinates(0, gbf.nodes, 0, gbf.nodesSize);
                        nodesArr.setColors(0, gbf.nodesColor, 0, gbf.nodesSize);
                        nodesAhspe3D = new Shape3D(nodesArr, attrNode);
                    }
                    gc.draw(nodesAhspe3D);
                }

                // 全体の回転
                t3dx.mul(0.0);
                t3dx.rotX(rotX * toRAD);
                t3dy.mul(0.0);
                t3dy.rotY(rotY * toRAD);
                t3dx.mul(t3dy);
                gc.setModelTransform(t3dx);

                // 2D描画
                J3DGraphics2D g2d = canvas.getGraphics2D();
                {
                    FontMetrics fm;
/*                    
                    g2d.setFont(fontLabel);
                    fm = g2d.getFontMetrics();
                    // Top10のノードにラベルを表示
                    int nStrHeight = fm.getHeight();
                    for(int i=0; i<gbf.top10Size; i++) {
                        int idx = gbf.top10[i];
                        // 3D座標から2D座標へ変換
                        posWorld.x = (double)gbf.nodes[idx * 3 + 2].x;
                        posWorld.y = (double)gbf.nodes[idx * 3 + 2].y;
                        posWorld.z = (double)gbf.nodes[idx * 3 + 2].z;
                        t3dx.transform(posWorld);	// ラベルも座標変換
                        canvas.getVworldToImagePlate(temp);
                        temp.transform(posWorld);
                        canvas.getPixelLocationFromImagePlate(posWorld, posScreen);

                        // ラベル表示
                        int nStrWidth = fm.stringWidth(gbf.strLabels[idx]);
                        g2d.setColor(colLabel);
                        g2d.fillRect((int)posScreen.x - 1, (int)posScreen.y - nStrHeight, nStrWidth + 4, nStrHeight + 2);
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(gbf.strLabels[idx], (float)posScreen.x, (float)posScreen.y);
                    }
*/
                    
                    // 固定2D画像描画
                    g2d.drawImage(imgGraph, 0, 0, this);

                    int y = 10;

                    // ロゴ表示
                    g2d.drawImage(imgATLLogo, 10, y, null);
                    y += imgATLLogo.getHeight(null);

                    // 日付、ノード数、エッジ数
                    y = posDateNodeEdge.y + 5;
                    String strCur = String.format("%s (%s ～ %s)", sdf.format(gbf.cur), sdf.format(gb.start), sdf.format(gb.end));
                    g2d.setFont(fontNodeAndEdge);
                    fm = g2d.getFontMetrics();
                    y += fm.getHeight();
                    arrNodeAndEdgeStr.clear();
                    arrNodeAndEdgeStr.add(strCur);
                    arrNodeAndEdgeStr.add(String.format("Nodes : %d, Edges : %d", gbf.nodeCnt, gbf.edgeCnt));
                    g2d.setColor(colNodeAndEdge);
                    for(String str : arrNodeAndEdgeStr) {
                        g2d.drawString(str, posDateNodeEdge.x + 10, y);
                        y += fm.getHeight() + 5;
                    }

                    // ロード済みフレーム数 & カメラ位置
                    if ((frameTop + frameRead) < gb.frameCnt) {
                        g2d.setFont(fontTop);
                        g2d.drawString(String.format("%4d (%4d / %4d ) cam ( x=%.1f, y=%.1f, z=%.1f )", readCnt, frameTop + frameRead, gb.frameCnt, vecCamera.x, vecCamera.y, vecCamera.z), 0, y - 5);
                    }

                    g2d.flush(true);
                }

                canvas.swap();
            }
        }

        @Override
        public void run() {
            while(!bStop) {
                render();
                try {
                    Thread.sleep(40);
                } catch(InterruptedException e) {
                }
            }
            System.out.println("thread end");
        }

        public void stop() {
            bStop = true;
            try {
                if (backThread != null)
                    backThread.join();
            } catch (InterruptedException e) {}
        }

        @Override
        public void componentResized(ComponentEvent e) {
            if (canvas != null) {
                Dimension size = getSize();
                canvas.setBounds(0, 0, size.width, size.height);
                controlPanel.setBounds(0, size.height-50, size.width, 50);
                playBtn.setBounds(10, 10, 50, controlPanel.getSize().height-20);
                seekBar.setBounds(70, 0, controlPanel.getSize().width - 70, controlPanel.getSize().height);
                int gh = (int)(size.height * 0.23) + 10;
                updownBar.setBounds(size.width - 20, gh, 20, size.height - controlPanel.getSize().height - gh);
                create2DImage(size);
            }
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getSource() == playBtn) {
                bPlay = !bPlay;
                if (bPlay) {
                    // 停止
                    playBtn.setIcon(btnIcon[1]);
                } else {
                    // 再生
                    playBtn.setIcon(btnIcon[0]);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getSource() == seekBar) {
                bWait = true;
                nSeekDrag = seekBar.getValue();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getSource() == seekBar) {
                synchronized (lock) {
                    nSeekDrag = seekBar.getValue();
                    readCnt = nSeekDrag;
                    main.setSeekFrame(readCnt);
                    gbf = null;
                }
                bWait = false;
            } else if (e.getSource() == updownBar) {
                Vector3d vec = getCameraPos();
                vec.z = (double)updownBar.getValue();
                setCameraPos(vec);
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (e.getSource() == updownBar) {
                Vector3d vec = getCameraPos();
                vec.z = (double)updownBar.getValue();
                setCameraPos(vec);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
        }

        @Override
        public void stateChanged(ChangeEvent e) {
        }
    }
}
