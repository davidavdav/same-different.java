package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.sound.*;
import javax.sound.sampled.*;



public class SDLang extends JFrame implements LineListener 
{
	private static final long serialVersionUID = 2L;
	static String datapath = "data";// or "P:/Orr/sd-lang/data/"; now in sd-lang.config
	static String task = "language"; // or "speaker"
	private String fIn, fOut;
	private JLabel tStatus = new JLabel();
	private Task current = null;
	private ArrayList<Task> agenda = new ArrayList<Task>();
	private final int Nconf = 5;
	int nr = 0, max, start, stop;
	long time=System.nanoTime();
	
	// content pane keeping all gui stuff
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	JPanel content = new JPanel(layout);
	
	// actual elements
	JButton[] bSame = new JButton[Nconf];
	JButton[] bDifferent = new JButton[Nconf];
	JButton bSampleA = new JButton("Sample A");
	JButton bSampleB = new JButton("Sample B");
	JButton bPause = new JButton("Pause");
	JButton bDone = new JButton("OK");
	
	String prop= task.equals("speaker") ? "by" : "in";
	
	JLabel tAnnounce = new JLabel(
			"<html><center>Are samples A and B spoken " + prop + " the <br>" +
			"<b>same</b> "+task+" or " + prop + " a <b>different</b> "+task+"?</html>", SwingConstants.CENTER
			);
	JLabel tPrior = new JLabel("<html><center>Note: 50% of the trials have the same "+task+"<br>" +
			"and 50% of the trials have a different "+task+"</html>");	
	
	private boolean voteAndProgress(float same) {
		// if there is a vote, write it
		if (current != null) {
			timestamp(same>0 ? "s" : "d");
			current.same = same;
			writeOut(current.toString());
		}
		// free audio resources
		if (clip1 != null) clip1.close();
		if (clip2 != null) clip2.close();
		// load next task
		nr++;
		if (nr <= stop) {
			current = agenda.get(nr);
			loadTask(current);
			bSampleA.setBackground(cNormal);
			bSampleB.setBackground(cNormal);
			return true;
		} else {
			kThxBye(false);
			current = null;
			return false;
		}
	}
	
	// this will display the thanks message, disabling all other controls...
	private void kThxBye(boolean thanks) {
//		JPanel p = new JPanel();
		JLabel l = new JLabel(thanks ? "Thank you for participating!" : "End of session");
		l.setFont(new Font("Arial", Font.BOLD, 32));
		content.removeAll();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0; c.gridy = 0; c.gridwidth = 9;
		c.ipady = 25;
		content.add(l, c);
		c.gridx = 0; c.gridy=1; c.gridwidth=9;
		content.add(bDone, c);
//		setContentPane(content);
		pack();
		setSize(800,640);
		setLocationRelativeTo(null);
	}
	
	private void writeOut(String msg) {
		try {
			BufferedWriter outf = new BufferedWriter(new FileWriter(fOut, true));
			outf.write(msg + "\n");
			outf.close();
		} catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
	}
	
	
	private void readFileList() {
		try {
			BufferedReader inf = new BufferedReader(new FileReader(fIn));
			String buf;
			while ((buf = inf.readLine()) != null)
				agenda.add(new Task(buf));
		} catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace();
		}
		System.err.println("there are " + agenda.size() + " items in the agenda");
		max = agenda.size();
//		voteAndProgress(false);
	}
	
	public SDLang(String infile, String outfile, String startstr, String stopstr) {
		setTitle("Experiment: " + infile);
		nr=start=Integer.valueOf(startstr).intValue();
		stop=Integer.valueOf(stopstr).intValue();
		fIn = infile;
		fOut = outfile;
		readFileList();
		current = agenda.get(nr);
		createComponents();
		loadTask(current);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800,640);
		setLocationRelativeTo(null);
		
		setResizable(true);
		setFont(new Font("Arial", Font.PLAIN, 12));
		setVisible(true);
	}
	
	private Color cNormal, cDark;	// default color
	private void createComponents() {
		
		String[] conf = {"very uncertain ",    // poor man's alignment...
						 "uncertain        ", 
						 "confident        ", 
						 "very confident", 
						 "certain           "};
		// specify button actions
		// bsame stack of options.
		int i;
		JPanel pSame = new JPanel(new GridLayout(Nconf,1));
		JPanel pDifferent = new JPanel(new GridLayout(Nconf,1));
		for (i=0; i<Nconf; i++) {
			bSame[i] = new JButton((i+1) + ": " + conf[i]+" Same");
			bDifferent[i] = new JButton((i+1) + ": " + conf[i] + " Different");
			final float v=(float) (i+0.5); // final, whatever that means...
			bSame[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					voteAndProgress(v);
				}
			});
			bDifferent[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					voteAndProgress(-v);
				}
			});
			pSame.add(bSame[i],0); //at top
			pDifferent.add(bDifferent[i],-1); // at bottom
		}
		
		
		bSampleA.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playA();
			}
		});
		
		bSampleB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				playB();
			}
		});
		
		bPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stop();
			}
		});
		
		bDone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		
		// record / prepare special button-pressed colors
		cNormal = bSampleA.getBackground();
		cDark = cNormal.darker();
		
		// add text announcement
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0; c.gridy = 0; c.gridwidth = 10;
		c.ipady = 25; c.ipadx=50;
		c.insets=new Insets(25,5,5,25);
		tAnnounce.setFont(new Font("Arial", Font.PLAIN, 18));
		content.add(tAnnounce, c);
		
		// add buttons
		c.gridx = 1; c.gridy = 1; c.gridwidth = 1;
		content.add(pSame, c);
		
		c.gridx = 0; c.gridy = 3; c.gridwidth = 1;
		content.add(bSampleA, c);
		
		JPanel pPause=new JPanel(new GridLayout());
		pPause.add(new JPanel());
		pPause.add(bPause);
		pPause.add(new JPanel());
		c.gridx = 1; c.gridy = 3; c.gridwidth = 1;
		content.add(pPause, c);
		
		c.gridx = 2; c.gridy = 3; c.gridwidth = 1;
		content.add(bSampleB, c);
		
		c.gridx = 1; c.gridy = 5; c.gridwidth = 1;
		content.add(pDifferent, c);
		
		c.gridx=0; c.gridy=6; c.gridwidth=3;
		tPrior.setFont(new Font("Arial", Font.PLAIN, 18));
		content.add(tPrior, c);
		
		// add status label
		c.gridx = 0; c.gridy = 7; c.gridwidth = 10;
//		c.anchor = c.CENTER;
		content.add(tStatus, c);
		
		setContentPane(content);
		pack();
	}
	
	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("usage: java -jar sd-lang.jar <experiment-file-in> <experiment-file-out> <first> <last>");
			System.exit(0);
		}
		Properties p = new Properties();
		try {
			p.load(new FileInputStream("sd-lang.config"));
			datapath=p.getProperty("data_path", datapath);
			task=p.getProperty("task", task);
		} catch (FileNotFoundException e) {
			System.err.println("No config file cound, using defaults");
		} catch(Exception e) {
			System.err.println(e);
			e.printStackTrace();			
		}
		SDLang inst = new SDLang(args[0], args[1], args[2], args[3]);
	}

	
	// audio IO
	private AudioInputStream ais1, ais2;
	private AudioFormat af1, af2;
	private Clip clip1, clip2;
	private DataLine.Info info1, info2;
	int pos1=0, pos2=0;
	private void loadTask(Task t) {
		setTitle(fIn + " (" + nr + " of " + max + ")");
		try {
			System.err.println("regenerating audio streams...");
			ais1 = AudioSystem.getAudioInputStream(new File(datapath + "/" + t.wavA + ".wav"));
			ais2 = AudioSystem.getAudioInputStream(new File(datapath + "/" + t.wavB + ".wav"));
			
			af1 = ais1.getFormat();
			af2 = ais2.getFormat();
			
			info1 = new DataLine.Info(Clip.class, af1);
			info2 = new DataLine.Info(Clip.class, af2);
			
			clip1 = (Clip) AudioSystem.getLine(info1);
			clip2 = (Clip) AudioSystem.getLine(info2);
			
			System.err.println("Opening audio files...");
			clip1.open(ais1);
			clip2.open(ais2);
			
			clip1.addLineListener(this);
			clip2.addLineListener(this);
						
			// ready to read from whatever file...
			tStatus.setText(nr + " / " + max);
			
			paused1 = paused2 = started1= started2 = false;
			update_choices();
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
	private void playA() {
		timestamp("A");
		if (clip2.isActive()) {
			pos2=clip2.getFramePosition();
			clip2.stop();
			clip2.setFramePosition(pos2);
			paused2=true;
		} 
		if (!paused1 && ! clip1.isActive())
			clip1.setFramePosition(0);
		clip1.start();
		paused1=false;
		started1=true;
		bSampleA.setBackground(cDark);
		bSampleB.setBackground(cNormal);
		update_choices();
	}
	private void playB() {
		timestamp("B");
		if (clip1.isActive()) {
			pos1=clip1.getFramePosition();
			clip1.stop();
			clip1.setFramePosition(pos1);
			paused1=true;
		} 
		if (!paused2 && ! clip2.isActive())
			clip2.setFramePosition(0);
		clip2.start();
		paused2=false;
		started2=true;
		bSampleB.setBackground(cDark);
		bSampleA.setBackground(cNormal);
		update_choices();
	}
	private void stop() {
		if (clip1.isActive()) {
			pos1=clip1.getFramePosition();
			clip1.stop();
			clip1.setFramePosition(pos1);
			paused1=true;
		}
		if (clip2.isActive()) {
			pos2=clip2.getFramePosition();
			clip2.stop();
			clip2.setFramePosition(pos2);
			paused2=true;
		}
	}
	
	private boolean paused1=false, paused2=false;
	private boolean started1=false, started2=false;
	
	private void update_choices() {
		boolean ok = started1 && started2;
		int i;
		for (i=0; i<Nconf; i++) {
			bSame[i].setEnabled(ok);	
			bDifferent[i].setEnabled(ok);
		}
	}
	
	private void timestamp(String s) {
		double secs = (double)(System.nanoTime()-time)/1e9;
		current.times.add(new String(s+":"+secs));
	}
	
	public void update(LineEvent e) {
		if (e.getType() == LineEvent.Type.STOP) {
//			Clip c = (Clip) e.getLine();
//			c.stop();
//			c.setFramePosition(0);
//			bSampleA.setBackground(cNormal);
//			bSampleB.setBackground(cNormal);		
//			System.err.println("Got an event..." + e.toString());			
		}
	}
}
