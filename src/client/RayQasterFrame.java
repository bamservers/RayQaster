package client;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.imageio.*;
import javax.swing.JFrame;
import javax.swing.UIManager;

// Renamed from Frame1 to give it a proper name
public class RayQasterFrame extends JFrame implements Runnable, KeyListener
{
	public Graphics offscreenG;
	public static Image offscreenImage;
	public Image Background;
	public Thread t;// = new Thread (this);
	//public Count c = new Count();
	public boolean Up = false;
	public boolean Down = false;
	public boolean Left = false;
	public boolean Right = false;
	public boolean ShowMiniMap = true;
	public int InWall = 0;
	public int k = 0;

	//Engine Variables
	public int Scale = 32;//32;
	public float PI = (float) Math.PI;
	public float FOV = PI / 2f;
	public int Maxx = 7;
	public int Maxy = 7;
	public final int maxx = 640;//320
	public final int maxy = 480;//240
	public float []SIN = new float [628318 + 1];//maximum is PI * 2
	public float []COS = new float [628318 + 1];
	public float []TAN = new float [628318 + 1];

	public float plrx = Scale * 3 + (Scale / 2) + .5f;
	public float plry = Scale * 3 + (Scale / 2);
	public float plrd = -PI / 4.3f ;
	public int [][] map = new int [Maxx][Maxy];
	public int Interlace = 50;
	public float sprx = Scale * 3 + (Scale / 2);
	public float spry = Scale * 4 + (Scale / 2);
	public int TEXSIZE = Scale;
	public int DrawMode = 2;
	boolean FullScreen = true;
	public Pic tex1;
	//public MemoryImageSource source = new MemoryImageSource(320, 200, offscreenbuffer, 0, 320);
	//public Image image = this.class.createImage(source);

	public static void main(String[] args) throws MalformedURLException
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

		RayQasterFrame frame = new RayQasterFrame();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height)
		{
			frameSize.height = screenSize.height;
		}
		if (frameSize.width > screenSize.width)
		{
			frameSize.width = screenSize.width;
		}
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			});
		frame.setVisible(true);
		frame.t = new Thread (frame);
		frame.t.start();
	}
	
	public void keyPressed (KeyEvent e)
		{
		int key = e.getKeyCode();
		if (key == KeyEvent.VK_ESCAPE)System.exit(0);
		if(key == KeyEvent.VK_TAB)ShowMiniMap = !ShowMiniMap;//true;//tab
		if(key == 39) Right = true;
		if(key == 38) Up = true;
		if(key == 40) Down = true;
		if(key == 37) Left = true;
		//VK_EQUALS == 61 while VK_PLUS == 521 (wtf???)
		//so from now on, forget about using Shift+Keys, just use keys themselves...
		//because when using key code, the code isn't ascii!!!
		//to get the ascii values, use e.getKeyChar()
		//when we just want to know physically what key is pressed, we use e.getKeyCode()
		if(key == KeyEvent.VK_EQUALS) Interlace++;//187
		if(key == KeyEvent.VK_MINUS && Interlace > 1) Interlace--;
		if(key == 32)
			{
			DrawMode++;
			if (DrawMode > 2)DrawMode = 0;
			}
		k = key;
		//System.out.println ("==" + KeyEvent.VK_EQUALS + "   +=" + KeyEvent.VK_PLUS);
		//System.out.println (k);

		}

	public void keyReleased (KeyEvent e)
		{
		int key = e.getKeyCode();
		//if(key == 9)ShowMiniMap = false;//tab
		if(key == 39) Right = false;
		if(key == 38) Up = false;
		if(key == 40) Down = false;
		if(key == 37) Left = false;
		if(key == 32);
		
		System.out.println ("Player: x=" + plrx + ", y=" + plry + ", d=" + plrd);
		}

	public void keyTyped (KeyEvent e){}


	public float FixDir (float d)
		{
		float r = d;
		if (r > PI * 2) r -= PI * 2;
		else if (r < 0) r += PI * 2;
		return r;
		}


	public float GetDist (float x1, float y1, float x2, float y2)
		{
		return (float) (Math.sqrt ( Math.pow ((x2 - x1), 2) + Math.pow ((y2 - y1), 2)));
		}
	public static int mod (float A, float B)
		{
		if (A > B)return (int) (A - (B * Math.floor(A / B)));
		else return (int) (B - (A * Math.floor(B / A)));
		//return (int)Math.floor(A) % (int)Math.floor(B);
		}
	public RayQasterFrame() throws MalformedURLException
	{
		try
		{
			jbInit();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		//set up graphics here for the ray casting
		if (FullScreen)this.setUndecorated(true);

		tex1 = new Pic (/*new URL(".") getDocumentBase(), */"wall2.bmp", this);

//		System.out.println ("W: " + tex1.width + " H: " + tex1.height);
		this.setFocusable(true);
		addKeyListener (this);
//		addMouseListener (this);
//		c.start();

		int a = 0;
		while (a < 628318 + 1)
			{
			SIN [a] = (float)Math.sin ((double)a / (double)100000);
			COS [a] = (float)Math.cos ((double)a / (double)100000);
			TAN [a] = (float)Math.tan ((double)a / (double)100000);
			a++;
			}
		//int test = 0;
		//t = new Thread (this);
		//t.start();
		
		
		// TODO: Reproduce the issue where some rays go through corners. Test case Player: x=97.27592, y=97.5203, d=3.9817894
//		plrx = 97.27592f;
//		plry = 97.5203f;
//		plrd = 3.9817894f;
//		Interlace = 141;
	}
	public void run ()
		{
		map [0][0] = 1;
		map [1][0] = 1;
		map [2][0] = 1;
		map [3][0] = 1;
		map [4][0] = 1;
		map [5][0] = 1;
		map [6][0] = 1;

		map [0][1] = 1;
		map [1][1] = 0;
		map [2][1] = 1;
		map [3][1] = 0;
		map [4][1] = 0;
		map [5][1] = 0;
		map [6][1] = 1;

		map [0][2] = 1;
		map [1][2] = 0;
		map [2][2] = 1;
		map [3][2] = 0;
		map [4][2] = 1;
		map [5][2] = 0;
		map [6][2] = 1;

		map [0][3] = 1;
		map [1][3] = 0;
		map [2][3] = 0;
		map [3][3] = 0;
		map [4][3] = 1;
		map [5][3] = 0;
		map [6][3] = 1;

		map [0][4] = 1;
		map [1][4] = 1;
		map [2][4] = 1;
		map [3][4] = 0;
		map [4][4] = 0;
		map [5][4] = 0;
		map [6][4] = 1;

		map [0][5] = 1;
		map [1][5] = 0;
		map [2][5] = 0;
		map [3][5] = 0;
		map [4][5] = 0;
		map [5][5] = 0;
		map [6][5] = 1;

		map [0][6] = 1;
		map [1][6] = 1;
		map [2][6] = 1;
		map [3][6] = 1;
		map [4][6] = 1;
		map [5][6] = 1;
		map [6][6] = 1;

		MakeBackground ();
		offscreenImage = createImage(maxx, maxy);
		offscreenG = offscreenImage.getGraphics();
		this.setFocusTraversalKeysEnabled(false);
		this.requestFocus();

		while (true)
			{
			//Keyboard Input
			if (Up && InWall != 1)
				{
//				float addx = (float)Math.sin (plrd) * 2;
//				float addy = (float)Math.cos (plrd) * 2;
				float addx = SIN [(int) Math.floor ((plrd * (double)100000))] * 2;
				float addy = COS [(int) Math.floor ((plrd * (double)100000))] * 2;
				if (map [(int)((float)(Math.floor ((plrx + addx)/ Scale)))][(int)((float)(Math.floor ((plry + addy)/ Scale)))] == 1)
					{
					InWall = 1;
					//% move back
					plrx -=	addx;
					plry -= addy;
					}
				else
					{
					InWall = 0;
					plrx += addx;
					plry += addy;
					}
				}
			else if (Down && InWall != -1)
				{
				//float addx = (float)Math.sin (plrd) * 2;
				//float addy = (float)Math.cos (plrd) * 2;
				float addx = SIN [(int) Math.floor ((plrd * (double)100000))] * 2;
				float addy = COS [(int) Math.floor ((plrd * (double)100000))] * 2;
				if (map [(int)((float)(Math.floor ((plrx - addx)/ Scale)))][(int)((float)(Math.floor ((plry - addy) / Scale)))] == 1)
					{
					InWall = -1;
					//% move back
					plrx += addx;
					plry += addy;
					}
				else
					{
					InWall = 0;
					plrx -= addx;
					plry -= addy;
					}
				}
			if (Left) plrd-= PI / 48;
			else if (Right) plrd+= PI / 48;

			//adjust the player angle to exist between 0 and 2*PI
			if (plrd >= 2 * PI) plrd -= 2 * PI;
			else if (plrd < 0) plrd += 2 * PI;

			repaint();
			//update (this.getGraphics());
			try {t.sleep(50);}
			catch (InterruptedException e){}
			}
		}//http://scv.bu.edu/Doc/Java/tutorial/ui/drawing/update.html

	private void jbInit() throws Exception
	{
		this.getContentPane().setLayout(null);
		this.setSize(new Dimension(maxx, maxy));
		this.setTitle("Ray Qaster");
	}

	public void MakeBackground ()
	{
		Background = createImage(maxx, maxy);
		Graphics bg = Background.getGraphics();

				//sky and ground
		float yy = 255f;
		int cnt = 0;
		while (yy <= 255)
			{
			if (yy < 0) yy = 0f;
			if (yy > 255) yy = 255f;
			bg.setColor (new Color ((int)yy, (int)yy, (int)yy));
			bg.fillRect (0, cnt, maxx, cnt);
			cnt++;
			if (cnt > maxy / 2)yy+= 0.75f;
			else yy-= 0.75f;
			}
	}
	public void paint (Graphics g)
	{
//        try
//        {
		if (offscreenG == null) return;
		offscreenG.setColor (Color.black);
		offscreenG.fillRect (0,0,maxx,maxy);
		offscreenG.setColor (Color.white);


		//g.drawLine(1, 22, this.getWidth(), this.getHeight());

		if (!FullScreen)offscreenG.drawRect(5, 23 , this.getWidth() - 11, this.getHeight() - 22 - 6);
		else offscreenG.drawRect(0, 0, this.getWidth() - 1, this.getHeight() - 1);


		if (Background != null) offscreenG.drawImage(Background, 0, 0, this);
		//draw the screen here
		Draw (offscreenG, plrx, plry, plrd);
		g.drawImage(offscreenImage, 0, 0, this);
  //      }
  //      catch (NullPointerException e){System.out.println ("WTFWTFWTF");e.printStackTrace();}
	}
	public void Draw (Graphics g, float x, float y, float dir)
		{
		float d = dir - (FOV / 2);
		int scr_x = 0;
		boolean DrawSprite = false;
		float MiniMapScale = 0.5f;

		if (ShowMiniMap)
			{
			//MINIMAP!!!!!!!!!!!!!!!!!!!

			g.setColor (Color.cyan);
			int a = 0;
			int b = 0;
			while (a < (Maxx * (Scale / MiniMapScale)))
				{
				while (b < (Maxy * (Scale / MiniMapScale)))
					{
					if (map [(int)Math.floor(a / (Scale / MiniMapScale))] [(int)Math.floor( b / (Scale / MiniMapScale))] == 1)
					   g.fillRect (a, b,(int)Math.floor(Scale / MiniMapScale),(int)Math.floor(Scale / MiniMapScale));
					b += Scale / MiniMapScale;
					}
				a += Scale / MiniMapScale;
				b = 0;
				}
			}

		//% PRE-CHECK IF SPRITE SHOULD BE DRAWN!!!!!!!!!!!!!!!!!!!
		int sx = 0;
		int size = 10;
		//% translates from 2D to 2.5D
		float X = (sprx - x);
		float Y = (spry - y);
		//% Determines the sine and cosine values and puts them in a variable
		//float SR1 = (float)Math.sin ( - dir);
		float SR1 = SIN [(int)(dir * 100000)];
		//float CR1 = (float)Math.cos ( - dir);
		float CR1 = COS [(int)(dir * 100000)];
		//float SR2 = (float)Math.sin ( - dir);
		//float SR2 = SIN [(int)(plrd * 1000)];
		//float CR2 = (float)Math.cos ( - dir);
		//float CR2 = COS [(int)(plrd * 1000)];
		//%Some algerbra for the 3D rotation
		//% Rotates around the Y axis
		float XA = (CR1 * X + SR1 * Y);
		float YA = (SR1 * X + CR1 * Y);

		//WITHOUT THESE TWO FOLLOWING LINES IT STILL WORKS!!!!!
		if (YA < .00001 && YA > - .00001) sx = (int)(maxx / 2);
		else sx = (int)( (FOV * 125) * XA / YA) + (int)(maxx / 2);

		//%Find out how to draw depending on player's direction
		//WITHOUT THESE FOLLOWING LINES IT STILL WORKS!!!!!
		if (dir > PI / 4 && dir < PI - (PI / 4) || dir > PI + (PI / 4) && dir < PI * 2 - (PI / 4))
			{
			sx -= maxx;
			if (sx < 0) sx *= -1;
			}

		int maxstep = (int)(Maxx * Math.pow (Scale, 2));

		while (true)
			{
			scr_x += Interlace;
			d += (FOV / (maxx + Interlace)) * Interlace;
			if (d >= dir + FOV || scr_x >= maxx + Interlace)break;
			float maxdist = 1000;
			float hdist = 1000;
			float vdist = 1000;
			int hitx = 999;
			int hity = 999;
			int ThisStripOnWallh = 0;
			int ThisStripOnWallv = 0;
			boolean IsWallH = false;
			boolean IsWallV = false;

			//%hori!, i know opp, i need adj!
			if ((d >= 0 && d <= PI) || d >= PI * 2)
				{
				int step = 0;//was a float
				while (true)
					{
					float opp = (Scale - mod (x, Scale) + step);
					float adj = opp / ((float)Math.tan (d));//this was originally commented

/*					int aa = (int) Math.floor ((d * (double)100000));
					if (aa < 0) aa += TAN.length;
					else if (aa > TAN.length - 1) aa -= TAN.length;

					float adj = opp / TAN [aa];
*/					hdist = GetDist (x + opp, adj + y, x, y);
					if (step > maxstep || adj > 1000 || adj < -1000) break;

					if ( Math.floor((opp + x) / Scale) < Maxx && Math.floor ( (opp + x) / Scale) > - 1)
						{
						if (Math.floor ( (adj + y) / Scale) < Maxy && Math.floor ( (adj + y) / Scale) > - 1)
							{
							if (map [(int)((float)(Math.floor ( (opp + x) / Scale)))] [(int)((float)(Math.floor ( (adj + y) / Scale)))] == 1)
								{
								g.setColor(Color.RED);
								g.drawString(".", (int)(opp + x), (int)(adj + y));
								//System.out.println ("Ray#=" + (int)(step / Scale) + " hdist=" + hdist);

								hitx = (int)((float)(Math.floor ( (opp + x) / Scale)));
								hity = (int)((float)(Math.floor ( (adj + y) / Scale)));
								int z = (int)(adj + y);
								while (z > 0) z -= Scale;
								ThisStripOnWallh = ((z * -1));
								if (mod (adj + y, Scale) < 0.5)IsWallH = true;
								break;
								}
							}
						}
					step += Scale;
					}
				}
			else
				{
				int step = 0;//was a float//float step = 0;
				while (true)
					{
					float opp = ((mod (x, Scale)) + step);
					opp *= - 1;

					float adj = opp / ((float)Math.tan (d));//this was originally commented
/*					int aa = (int) Math.floor ((d * (double)100000));
					if (aa < 0) aa += TAN.length;
					else if (aa > TAN.length - 1) aa -= TAN.length;
					float adj = opp / TAN [(int) Math.floor (aa)];
*/
					hdist = GetDist (x + opp, adj + y, x, y);
					if (step > maxstep || adj > 1000 || adj < -1000) break;

					if (Math.floor ( (opp + x) / Scale) - 1 < Maxx && Math.floor ( (opp + x) -1 / Scale)> 0)//changed -1 to 0
						{
						if (Math.floor ( (adj + y) / Scale) < Maxy && Math.floor ( (adj + y) / Scale) > - 1)
							{
							if (map [(int)((float)(Math.floor ( (opp + x) / Scale) - 1))] [(int)((float)(Math.floor ( (adj + y) / Scale)))] == 1)
								{
								g.setColor(Color.RED);
								g.drawString(".", (int)(opp + x), (int)(adj + y));

								hitx = (int)((float)(Math.floor ( (opp + x) / Scale) - 1));
								hity = (int)((float)(Math.floor ( (adj + y) / Scale)));
								int z = (int)(adj + y);
								while (z > 0) z -= Scale;
								ThisStripOnWallh = ((z + Scale));
								if (mod (adj + y, Scale) < 0.5)IsWallH = true;
								break;
								}
							}
						}
					step += Scale;
					}
				}

			//%verti!, i know adj, i need opp!
			if ((d > PI * 3 / 2 && d < PI * 2) || (d > 0 && d < PI / 2) || d > PI * 2 || d < 0)
				{
				int step = 0;//was a float//float step = 0;
				while (true)
					{
					float adj = (Scale - (mod (y, Scale)) + step);
					if (adj < 0) adj *= -1;//make adj positive
					float opp = adj * ((float)Math.tan (d));//this was originally commented

 /*                   int aa = (int) Math.floor ((d * (double)100000));
					if (aa < 0) aa += TAN.length;
					else if (aa > TAN.length - 1) aa -= TAN.length;
					float opp = adj * TAN [aa];
*/
					vdist = GetDist (opp + x, adj + y, x, y);
					if (step > maxstep || opp > 1000 || opp < - 1000) break;

					if (Math.floor ( (opp + x) / Scale) < Maxx && Math.floor ( (opp + x) / Scale) > - 1)
						{
						if (Math.floor ( (adj + y) / Scale) < Maxy && Math.floor ( (adj + y) / Scale) > - 1)
							{
							if (map [(int)((float)(Math.floor ( (opp + x) / Scale)))] [(int)((float)(Math.floor ( (adj + y) / Scale)))] == 1)
								{
								g.setColor(Color.RED);
								g.drawString(".", (int)(opp + x), (int)(adj + y));

								hitx = (int)((float)(Math.floor ( (opp + x) / Scale)));
								hity = (int)((float)(Math.floor ( (adj + y) / Scale)));
								int z = (int)(opp + x);
								while (z > 0) z -= Scale;
								ThisStripOnWallv = ((z + Scale));
								if (mod (opp + x, Scale) < 0.5)IsWallV = true;
								break;
								}
							}
						}
					step += Scale;
					}
				}
			else
				{
				int step = 0;//was a floatfloat step = 0;
				while (true)
					{
					//float adj = (float)(y - Scale * Math.floor (y / Scale) + step);//change this to use the mod function!!
					float adj = (float)((mod (y, Scale)) + step);
					adj *= - 1;
					float opp = adj * ((float)Math.tan (d));//this was originally commented

/*					int aa = (int) Math.floor ((d * (double)100000));
					if (aa < 0) aa += TAN.length;
					else if (aa > TAN.length - 1) aa -= TAN.length;
					float opp = adj * TAN [aa];
*/
					vdist = GetDist (opp + x, adj + y, x, y);
					if (step > maxstep|| opp > 1000 || opp < - 1000) break;

					if (Math.floor ( (opp + x) / Scale) < Maxx && Math.floor ( (opp + x) - 1 / Scale)  > - 1)
						{
						if (Math.floor ( (adj + y) / Scale) - 1 < Maxy && Math.floor ( (adj + y) / Scale) - 1 > - 1)
							{
							if (map [(int)((float)(Math.floor ( (opp + x) / Scale)))] [(int)((float)(Math.floor ( (adj + y) / Scale) - 1))] == 1)
								{
								g.setColor(Color.RED);
								g.drawString(".", (int)(opp + x) - 1, (int)(adj + y) - 1);

								hitx = (int)((float)(Math.floor ( (opp + x) / Scale)));
								hity = (int)((float)(Math.floor ( (adj + y) / Scale) - 1));
								int z = (int)(opp + x);
								while (z > 0) z -= Scale;
								ThisStripOnWallv = ((z * -1));
								if (mod (opp + x, Scale) < 0.5) IsWallV = true;
								break;
								}
							}
						}
					step += Scale;
					}
				}

				boolean useVStrip = false;
				if (vdist <= hdist)
					{
					maxdist = vdist;
					useVStrip = true;
					}
				else maxdist = hdist;
				maxdist = maxdist / 2;
				
				
				if (maxdist < 1000)
					{
					//% draw horizontal line for real world
					// (Math.round ( maxdist / 200 * 255))

					int col = (int)Math.round(255 - (maxdist * 1.2));
					if (col < 0) col = 0;

					// TODO: Use for debugging: System.out.println ("Ray @ " + scr_x + ": " + vdist + ", " + hdist);
					
					
					//% cast ray in mini map
					   if (ShowMiniMap)
						{
						g.setColor (Color.yellow);
						g.drawLine ((int)((float)(Math.floor (x / MiniMapScale))), (int)((float)(Math.floor (y / MiniMapScale))), (int)((float)(Math.floor ( (x + Math.sin (d) * (maxdist * 2)) / MiniMapScale))), (int)((float)(Math.floor ( (y + Math.cos (d) * (maxdist * 2)) / MiniMapScale))));

						}
					else
					if (DrawMode == 0)//wireframe
						{
						int a = (int)Math.round(255 - (maxdist * 1.2));
						if (a < 0) a = 0;
						g.setColor (new Color (a, a, a));
						int top = (int)((float)Math.floor ( (1 / maxdist) * (Scale * 30)));
						int bottom = (int)((float)Math.floor (maxy / 2 - ( (1 / maxdist) * (Scale * 30))));
						g.drawString (".", scr_x, top+ 100);
						g.drawString (".", scr_x, bottom);
						if (IsWallV) g.drawLine (scr_x - Interlace, top + 100, scr_x - Interlace, bottom);
						else if (IsWallH) g.drawLine (scr_x - Interlace, top + 100, scr_x - Interlace, bottom);
						}
					else if (DrawMode == 2)//solid
						{
						//MemoryImageSource zz = new MemoryImageSource (
						g.setColor (new Color (col, col, col));
						g.fillRect (scr_x - Interlace, (int)((float)Math.floor (maxy / 2 - ( (1 / maxdist) * (Scale * 30)))),  Interlace, (int)((float)Math.floor ( (1 / maxdist) * (Scale * 30)) * 2));
						}
					else if (DrawMode == 1)//textured
						{
						int Ycoord = (int)((float)Math.floor (maxy / 2 - ( (1 / maxdist) * (Scale * 30))));
						int WallHeight = (int)((float)Math.floor ( (1 / maxdist) * (Scale * 30)) * 2);
						
						
						
						if (useVStrip)tex1.DrawVirtStrip (offscreenG, tex1.ResizeStrip (WallHeight, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallv)), scr_x, Ycoord, Interlace);
						else tex1.DrawVirtStrip (offscreenG, tex1.ResizeStrip (WallHeight, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallh)), scr_x, Ycoord, Interlace);
						if (useVStrip) tex1.DrawVirtStrip (this.getGraphics(), tex1.ResizeStrip (WallHeight, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallv)), scr_x, Ycoord, Interlace);
						else tex1.DrawVirtStrip (this.getGraphics(), tex1.ResizeStrip (WallHeight, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallh)), scr_x, Ycoord, Interlace);

						if (useVStrip)tex1.ResizeAndDrawVirtStrip (offscreenG, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallv), scr_x - Interlace, Ycoord, Interlace, WallHeight);
						else tex1.ResizeAndDrawVirtStrip (offscreenG, tex1.GetVirtStrip (this.TEXSIZE, ThisStripOnWallh), scr_x - Interlace, Ycoord, Interlace, WallHeight);

						}
					else
						{
						g.setColor (Color.yellow);
						g.drawString ("Unknown Draw Mode", 100, 100);
						}
					if (scr_x <= sx && scr_x + Interlace >= sx)
						{
						float sprdist = GetDist (sprx, spry, x, y);
						if (maxdist >= sprdist) DrawSprite = true;
						}
					}
				}
			if (DrawSprite)
				{
				sx = 0;
				//% translates from 2D to 2.5D
				X = (sprx - plrx);
				Y = (spry - plry);
				//% Determines the sine and cosine values and puts them in a variable
				SR1 = SIN [(int)(dir * 100000)];
				CR1 = COS [(int)(dir * 100000)];
				//SR2 = (float)Math.sin ( - dir);
				//CR2 = (float)Math.cos ( - dir);
				//%Some algerbra for the 3D rotation
				//% Rotates around the Y axis
				XA = (CR1 * X + SR1 * Y);
				YA = (SR1 * X + CR1 * Y);
				X = XA;
				Y = YA;
				//%    put dir / PI * 180

				if (Y < .00001 && Y > - .00001) sx = (int)(maxx / 2);
				else sx = (int) ( (FOV * 125) * X / Y) + (int)(maxx / 2);


				size = (int) ( (1 / (GetDist (sprx, spry, x, y) + 1)) *
					(Scale * 30));
				size = size / 2;
				//%    put sx

				//%Find out how to draw depending on player's direction

				//%           45 to 135 or 225 to 315
				g.setColor (Color.red);
				if (dir > PI / 4 && dir < PI - (PI / 4) || dir > PI + (PI / 4) && dir < PI * 2 - (PI / 4))
					{
					if (Y < 0)
						{
						sx -= maxx;
						if (sx < 0) sx *= -1;
						if (sx < 320)g.fillRect (sx, (maxy / 2), size, size);
						}
					}
				else
					{
					if (Y > 0) g.fillRect (sx, (maxy / 2), size, size);
					}
				}
			}

	public Image getImage(String filename) throws IOException
	{
		return ImageIO.read(new java.io.File (filename));
	}
}

class Pic// implements ImageObserver
{
public RayQasterFrame parent;
public int[] pixels;
public int height = -1;
public int width = -1;
int d_GVS = 0;
int d_RS = 0;
int d_DVS = 0;
int d_RADVS = 0;

public Pic (String filename, RayQasterFrame f)
	{
	parent = f;
	Image tex = null;
	
	try
	{
		tex = parent.getImage (filename);
	}
	catch (Exception e)
	{
		System.out.println ("Unable to load texture: " + e.toString());
		e.printStackTrace();
	}
	
	if (tex != null)
	{
		PixelGrabber b = new PixelGrabber (tex, 0, 0, parent.TEXSIZE, parent.TEXSIZE, true);
		height = b.getHeight();
		width = b.getWidth();
		
		pixels = new int [width*height];
		try
			{
			if (b.grabPixels())
				{
				pixels = (int[])b.getPixels();		
				}
			}
		catch (InterruptedException i)
			{
			System.out.println("Pixel grabber interrupted");
			}
	}
	}

public void Draw (Graphics g, int x, int y)
	{
	int cntx = 0;
	int cnty = 0;
	while (cntx < width)
		{
		g.setColor (new Color (pixels[(cnty * width) + cntx]));
		//g.drawRect (cnty + y, cntx + x, 1, 1);
		g.drawString (".", cntx + x, cnty + y);
		cnty++;
		if (cnty > height - 1)
			{
			cntx++;
			cnty = 0;
			}
		}
	}

public int [] GetVirtStrip (int length, int whichstrip)
	{
	int [] result = new int [length];
	int cnt = 0;
	if (whichstrip >=parent.TEXSIZE) whichstrip -= parent.TEXSIZE;
	while (cnt < height)//don't make it render the pixels u can't see!
		{
		result [cnt] = pixels [(cnt * width) + whichstrip];
		cnt++;
		}
	d_GVS += cnt;//incrememnt the # of cycles
	return result;
	}

public int[] ResizeStrip (int stretchto, int []strip)
	{
	int maxstrip = strip.length;
	int [] result = new int [stretchto];
	float inc = (float)maxstrip / (float)stretchto;
	int cnt1 = 0;
	float cnt2 = 0;
	while (cnt1 < stretchto)//don't make it render the pixels u can't see!
		{
		result [cnt1] = strip [(int)cnt2];
		cnt2 += inc;
		cnt1++;
		}
	d_RS += cnt1;//incrememnt the # of cycles
	return result;
	}

public void DrawVirtStrip (Graphics g, int []strip, int x, int y, int wid)
	{
	int cnt = 0;
	int cnt2 = 0;
	int cnt3 = 0;//used to count the # of cycles
	while (cnt2 < wid)
		{
		while (cnt < strip.length)
			{
			g.setColor (new Color (strip[cnt]));
			//g.drawRect (x + cnt2 - wid, cnt + y, 1, 1);
			//g.drawOval (x + cnt2, cnt + y, 1, 1);
			g.drawString (".", x + cnt2, cnt + y);
			cnt++;
			}
		d_DVS += cnt;//incrememnt the # of cycles
		cnt = 0;
		cnt2++;
		}
	}

//combine the above 2 methods into one big method
//this big method is what lags the whole thing!
public void ResizeAndDrawVirtStrip (Graphics g, int []strip, int x, int y, int wid, int stretchto)
	{
	int maxstrip = strip.length;
	float inc = (float)maxstrip / (float)stretchto;
	int cnt1 = 0;
	
	int startpix = (int) ((stretchto / 2) - (parent.maxy / 2));
	int endpix = (int) ((stretchto / 2) + (parent.maxy / 2));
	if (startpix < 0) startpix *= -1;
	if (endpix < 0) endpix *= -1;
	if (stretchto < parent.maxy + 1)
		{
		startpix = 0;
		endpix = stretchto;
		}
	else
		{
		cnt1 = startpix;
		//inc = (float)maxstrip / (float)parent.maxy;
		}
		
	float cnt2 = inc * startpix;
	int cnt3 = wid - 1;//change to 0 to make it look nicer
	while (cnt3 < wid)
		{
		while (cnt1< endpix)//endpix//don't make it render the pixels u can't see!
			{
			if (cnt1 > startpix && cnt1 < endpix || stretchto < parent.maxy + 1)
				{
				g.setColor (new Color (strip [(int)cnt2]));
//				if (cnt1 + startpix < stretchto - endpix)//strip is NOT out of bounds!
//					{
				g.drawRect (x + cnt3 - wid, cnt1 + y, wid, 1);
//					}
				//g.drawOval (x + cnt3, cnt1 + y, 1, 1);
				//g.drawString (".", x + cnt3, cnt1 + y);
				//cnt++;

				cnt2 += inc;
				d_RADVS += cnt1;
				}
			cnt1++;
			}
		cnt1 = 0;
		cnt2 = 0;
		cnt3++;
		}
	}	
}
