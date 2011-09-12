import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SlidePazzle {
	private static final String CHARSET = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ0";
	private static final char SPACE = '0';
	private static final char BLOCK = '=';
	private static final File DIR_RES = new File("D:/workspace/Pazzle/resource");
	private Settings settings;
	private List<Pazzle> pazzleList;
	private List<String> ansList = new AddArrayList<String>() {
		private static final long serialVersionUID = 1L;
		@Override
		public synchronized String set(int index, String element) {
			return super.set(index, element);
		}
	};
	public static void main(String[] args) throws Exception {
		SlidePazzle sp = new SlidePazzle();
		sp.execute();
	}
	
	class PazzleRunner implements Runnable {
		private Pazzle pzl;
		public PazzleRunner(Pazzle pzl) {
			this.pzl = pzl;
		}
		@Override
		public void run() {
			pzl.createAnswer();
		}
	}
	
	private int getAliveThreadCount(List<Thread> thList) {
		int thcount = 0;
		for (Thread cth : thList) {
			if (cth.isAlive()) {
				thcount++;
			}
		}
		return thcount;
	}
	private void execute() {
		// 入力ファイル読み込み
		loadInputFile();
		BufferedWriter writer = null;
		List<Thread> threadList = new ArrayList<Thread>();
		Collections.reverse(pazzleList);
		for (Pazzle pzl : pazzleList) {
			if (pzl instanceof PazzleDummy) {
				pzl.createAnswer();
				continue;
			}
			threadList.add(new Thread(new PazzleRunner(pzl)));
		}
		System.out.println(threadList.size());
		int thLimit = 2;
		for (Thread th : threadList) {
			th.start();
			while (getAliveThreadCount(threadList) >= thLimit) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		for (Thread th : threadList) {
			while (th.isAlive()) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			writer = new BufferedWriter(new FileWriter(new File(DIR_RES, "output.txt")));
			Collections.reverse(pazzleList);
			for (Pazzle pzl : pazzleList) {
				writer.write(pzl.answer);
				writer.write("\r\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Pazzle getPazzle(String line) {
		String[] tokens = line.split(",");
		int x = Integer.parseInt(tokens[0]);
		int y = Integer.parseInt(tokens[1]);
		if (x*y <= 9) {
			return new PazzleBreadth(line);
//			return new PazzleDepth(line);
		} else if(x*y == 20) {
			return new PazzleDepth(line);
		}
		
		return new PazzleDummy("");
	}
	
	private void loadInputFile() {
		BufferedReader reader = null;
		BufferedReader output = null;
		try {
			reader = new BufferedReader(new FileReader(new File(DIR_RES, "input.txt")));
			output = new BufferedReader(new FileReader(new File(DIR_RES, "output.txt")));
			settings = new Settings(reader.readLine(), reader.readLine());
			pazzleList = new ArrayList<Pazzle>();
			String line = null;
			String out = null;
			int count = 1;
			int nodummycount = 1;
			while ((line = reader.readLine()) != null) {
				out = output.readLine();
				Pazzle add = null;
				if (out == null || out.isEmpty()) {
					add = getPazzle(line);
					if (!(add instanceof PazzleDummy)) {
						nodummycount++;
						System.out.println("!"+count+" = "+line);
					} else {
						System.out.println(count+" = "+line);
					}
				} else {
					// 既に回答済み
					System.out.println(count + "\t" + out);
					add = new PazzleDummy(out);
				}
				add.setNo(count);
				pazzleList.add(add);
				count++;
			}
			System.out.println("nodumy="+nodummycount);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("read err.", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	abstract class Pazzle {
		int no;
		String answer;
		public void createAnswer() {
			if (this instanceof PazzleDummy) {
				ansList.set(no, answer);
				return;
			}
			System.out.println(no + " search start.");
			answer = search();
			if (answer == null || answer.isEmpty()) {
				System.out.println(no + ":cannnot answer");
				answer = "";
			} else {
				System.out.println(no + "\t" + answer);
			}
			ansList.set(no, answer);
		}
		abstract String search();
		void setNo(int no) {
			this.no = no;
		}
	}
	class PazzleDummy extends Pazzle {
		public PazzleDummy(String ans) {
			answer = ans;
		}
		@Override
		public String search() {
			return answer;
		}
	}

	class PazzleDepth extends Pazzle {
		private final int BOARD_X;
		private final int BOARD_Y;
		// 初期ボード
		private final Board start;
		// 現在ボード
		private Board current;
		private List<Point> movePosList;
		private List<Point> movetoPosList;

		public String search() {
			movePosList = new AddArrayList<Point>();
			movetoPosList = new AddArrayList<Point>();
			movePosList.add(null);
			movetoPosList.add(null);
			current = start.copy();
			
//			System.out.println(current.getSearchDistance(end));
//			System.out.println(current.toStringDistance());
			
			// 各設問5分制限
			Calendar c = Calendar.getInstance();
			c.add(Calendar.MINUTE, 5);
			Date limitDate = c.getTime();
			Date startDate = new Date();
			int low = start.getSearchDistance();
			for (int i=low; i<90;i+=2) {
				String ans = search(i, 0, start.getSpacePosition(), low);
				if (ans != null) {
					System.out.println("resolve time[" + startDate + "=>" + new Date() + "]");
					return ans;
				}
				if (limitDate.compareTo(new Date()) < 0) {
					System.out.println("timeout " + no + "[" + i + "]");
					return null;
				}
			}
			System.out.println("over limit."+no);
			return null;
		}
		public String search(int limit, int move, Point spacePos, int low) {
			if (low == 0) {
				return getAnswer();
			}
			// 次のゴール距離でソートして短いものから処理する
			Map<Integer, List<Point>> sortAdjaceMap = new TreeMap<Integer, List<Point>>();
			for (Point adjace : current.adListMap.get(spacePos)) {
				if (adjace.equals(movetoPosList.get(move))) {
					continue;
				}
				int next = current.getNextSearchDistance(spacePos, adjace);
				List<Point> pList = sortAdjaceMap.get(next);
				if (pList == null) {
					pList = new ArrayList<Point>();
					sortAdjaceMap.put(next, pList);
				}
				pList.add(adjace);
			}
			for (Entry<Integer, List<Point>> entry : sortAdjaceMap.entrySet()) {
				int newLow = entry.getKey();
				for (Point adjace : entry.getValue()) {
					// 移動
					current.replace(spacePos, adjace);

					movePosList.set(move+1, adjace);
					movetoPosList.set(move+1, spacePos);
					
					// 下限値枝刈り
					if (newLow + move<= limit) {
						// 再帰
						String ans = search(limit, move+1, adjace, newLow);
						if (ans != null) {
							return ans;
						}
					}

					// 元に戻す
					current.replace(adjace, spacePos);
				}
			}
			return null;
		}
		

		public PazzleDepth(String line) {
			String[] tokens = line.split(",");
			BOARD_X = Integer.parseInt(tokens[0]);
			BOARD_Y = Integer.parseInt(tokens[1]);
			start = new Board(BOARD_X, BOARD_Y, tokens[2]);
		}
		public String getAnswer() {
			StringBuilder ans = new StringBuilder();
			for (Point movePos : movePosList) {
				if (movePos != null) {
					ans.append(((SpacePoint) movePos).direction);
				}
			}
			return ans.toString();
		}
	}
	
	class PazzleBreadth extends Pazzle {
		private final Integer FORARD = new Integer(0);
		private final Integer BACKWARD = new Integer(1);
		private final int BOARD_X;
		private final int BOARD_Y;
		// 初期ボード
		private final Board start;
		// 完成ボード
		private final Board end;
		private final List<Board> prevState = new AddArrayList<Board>();;
		private final List<Point> spacePosList = new AddArrayList<Point>();
		private final List<Integer> directionList = new AddArrayList<Integer>();
		
		public String search() {
			int front = 0;
			int rear = 2;
			// 両側検索を行う
			prevState.set(0, start.copy());
			prevState.set(1, end.copy());
			directionList.set(0,FORARD);
			directionList.set(1, BACKWARD);
			spacePosList.add(0, start.getSpacePosition());
			spacePosList.add(1, end.getSpacePosition());

			Board next;
			Point spacePos;
			while (front < rear) {
				spacePos = spacePosList.get(front);
				for (Point adjace : start.adListMap.get(spacePos)) {
					// 次のボード生成
					next = prevState.get(front).copy();
					// スペースと値入れ替え
					next.replace(adjace, spacePos);

					// 値の保持
					int sameState = prevState.indexOf(next);
					spacePosList.set(rear, adjace);
					next.prev = front;
					prevState.set(rear, next);
					directionList.set(rear, directionList.get(front));
					// 終了判定
					if (sameState != -1) {
						if (!directionList.get(sameState).equals(directionList.get(rear))) {
							return getAnswer(sameState, rear);
						}
					} else if (sameState == -1) {
						rear++;
						if(rear > 300000){
							return null;
						}
					}
				}
				front++;
			}
			return null;
		}
		

		public PazzleBreadth(String line) {
			String[] tokens = line.split(",");
			BOARD_X = Integer.parseInt(tokens[0]);
			BOARD_Y = Integer.parseInt(tokens[1]);
			start = new Board(BOARD_X, BOARD_Y, tokens[2]);
			StringBuilder sb = new StringBuilder();
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					char c = start.get(x, y);
					// 壁はそのまま
					if (c == BLOCK) {
						sb.append(c);
					} else {
						sb.append(CHARSET.charAt(y*BOARD_X+x));
					}
				}
			}
			// 右下は空固定
			sb.setCharAt(sb.length()-1, SPACE);
			end = new Board(BOARD_X, BOARD_Y, sb.toString(), start.adListMap, start.distanceMap);
			end.board = end.end;
			// 右下は空固定
			end.set(BOARD_X-1, BOARD_Y-1, SPACE);
//			System.out.println(start);
//			System.out.println(end);
		}
		public String getAnswer(int sameState, int rear) {
//			System.out.println(directionList.get(sameState) + ":" + sameState + "=" + rear);
			StringBuilder ans = new StringBuilder();
			if (directionList.get(sameState).equals(FORARD)) {
//				System.out.println("=============FORWARD======================");
				ans.append(getForard(sameState));
//				System.out.println("=============BACKWARD======================");
				ans.append(getBackward(rear));
			} else if (directionList.get(sameState).equals(BACKWARD)){
//				System.out.println("=============FORWARD======================");
				ans.append(getForard(rear));
//				System.out.println("=============BACKWARD======================");
				ans.append(getBackward(sameState));
			}
			return ans.toString();
		}

		public String getForard(int rear) {
			Board b = prevState.get(rear);
			SpacePoint p = (SpacePoint) spacePosList.get(rear);
			List<String> sList = new ArrayList<String>();
			List<Integer> pList = new ArrayList<Integer>();
			pList.add(rear);
			while (b.prev != 0) {
				sList.add(p.direction);
				pList.add(b.prev);
				p = (SpacePoint) spacePosList.get(b.prev);
				b = prevState.get(b.prev);
			}
//			System.out.println(pList);
			sList.add(p.direction);
			StringBuilder move = new StringBuilder();
			for (int i=sList.size()-1; i>=0; i--) {
				move.append(sList.get(i));
			}
//			System.out.println(sList);
			return move.toString();
		}
		public String getBackward(int rear) {
			Board b = prevState.get(rear);
			SpacePoint p = (SpacePoint) spacePosList.get(rear);
			List<String> sList = new ArrayList<String>();
			List<Integer> pList = new ArrayList<Integer>();
			pList.add(rear);
			while (b.prev != 0) {
				sList.add(p.backdirection);
				pList.add(b.prev);
				p = (SpacePoint) spacePosList.get(b.prev);
				b = prevState.get(b.prev);
			}
//			System.out.println(pList);
			// backwardなので不要
//			sList.add(p.backdirection);
			StringBuilder move = new StringBuilder();
			for (String s : sList) {
				move.append(s);
			}
//			System.out.println(sList);
			return move.toString();
		}
	}
	class Board {
		public final int BOARD_X;
		public final int BOARD_Y;
		int prev;
		// 現在のボード
		char[][] board;
		// 最終ボード
		char[][] end;
		private final Map<Character, int[][]> distanceMap;
		// 隣接リスト
		private final Map<Point, List<Point>> adListMap;
		// 移動済みキャッシュ
		private Map<String, Integer> moveCache = new HashMap<String, Integer>();
		
		public Board(int sx, int sy, String line, Map<Point, List<Point>> adLM, Map<Character, int[][]> dMap) {
			BOARD_X = sx;
			BOARD_Y = sy;
			board = new char[BOARD_Y][BOARD_X];
			end = new char[BOARD_Y][BOARD_X];
			if (line != null) {
				for (int y=0; y<BOARD_Y; y++) {
					for (int x=0; x<BOARD_X; x++) {
						char c = line.charAt(y*BOARD_X+x);
						board[y][x] = c;
						// 壁はそのまま
						if (c == BLOCK) {
							end[y][x] = c;
						} else {
							end[y][x] = CHARSET.charAt(y*BOARD_X+x);
						}
					}
				}
				// 右下は空固定
				end[BOARD_Y-1][BOARD_X-1] = SPACE;
			}

			if (adLM != null) {
				this.adListMap = adLM;
			} else {
				this.adListMap = new HashMap<Point, List<Point>>();
				for (int y=0; y<BOARD_Y; y++) {
					for (int x=0; x<BOARD_X; x++) {
						List<Point> adList = new ArrayList<Point>();
						if (y > 0 && !isBlock(x, y-1)) {
							adList.add(new SpacePoint(x, y-1, "U", "D"));
						}
						if (y < BOARD_Y-1 && !isBlock(x, y+1)) {
							adList.add(new SpacePoint(x, y+1, "D", "U"));
						}
						if (x > 0 && !isBlock(x-1, y)) {
							adList.add(new SpacePoint(x-1, y, "L", "R"));
						}
						if (x < BOARD_X-1 && !isBlock(x+1, y)) {
							adList.add(new SpacePoint(x+1, y, "R", "L"));
						}
						this.adListMap.put(new Point(x, y), adList);
					}
				}
			}
			if (dMap != null) {
				this.distanceMap = dMap;
			} else {
				this.distanceMap = new HashMap<Character, int[][]>();
				// 完成までの最小を求める
				initDistance();
			}
			
		}

		public Board(int sx, int sy, String line) {
			this(sx, sy, line, null, null);
		}
		public char[][] getEnd() {
			return end;
		}
		public void set(int x, int y, char value) {
			board[y][x] = value;
		}
		public void replace(Point current, Point next) {
			char tmp = board[next.y][next.x];
			board[next.y][next.x] = board[current.y][current.x];
			board[current.y][current.x] = tmp;
		}
		public Board copy() {
			Board copy = new Board(BOARD_X, BOARD_Y, null, adListMap, distanceMap);
			copy.end = end;
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					copy.set(x, y, get(x, y));
				}
			}
			return copy;
		}
		/**
		 * 移動済みキャッシュをチェックし、過去に移動済みを判定する
		 * @param next 次の移動先
		 * @param move 現在の移動回数
		 * @return 移動するならfalse、移動不要ならtrue
		 */
		public boolean checkMoved(Point current, Point next, int move) {
			String nextString = toNextString(current, next);
			Integer minMove = moveCache.get(nextString);
			if (minMove == null) {
				minMove = new Integer(Integer.MAX_VALUE);
				moveCache.put(nextString, minMove);
			}
			if (minMove <= move) {
				System.out.println("moved." + nextString);
				return true;
			}
			moveCache.put(nextString, minMove);
			return false;
		}
		public Point getSpacePosition() {
			for (int i=0; i<BOARD_Y; i++) {
				for (int j=0; j<BOARD_X; j++) {
					if (board[i][j] == SPACE) {
						return new SpacePoint(j, i, "-", "-");
					}
				}
			}
			throw new RuntimeException("スペースがないよー？");
		}
		public char get(int x, int y) {
			return board[y][x];
		}
		private void setupdistance(int[][] d, int ix, int iy) {
			LinkedList<Point> stateQueue = new LinkedList<Point>();
			// 初期位置
			stateQueue.add(new Point(ix, iy));
			Point prevPos;
			// キューがなくなるまでループ
			while ((prevPos = stateQueue.poll()) != null) {
				for (Point adjace : adListMap.get(prevPos)) {
					// 設定済みならスルー
					if (d[adjace.y][adjace.x] != Integer.MAX_VALUE) {
						continue;
					}
					// 前の値+1を設定
					d[adjace.y][adjace.x] = d[prevPos.y][prevPos.x]+1;
					// キューに追加
					stateQueue.add(adjace);
				}
			}
		}
		
		private void initDistance() {
			for (char target : CHARSET.substring(0, BOARD_X*BOARD_Y-1).concat("0").toCharArray()) {
				int[][] distance = new int[BOARD_Y][BOARD_X];
				// 初期値設定
				int tx = -1;
				int ty = -1;
				for (int ey=0; ey<BOARD_Y; ey++) {
					for (int ex=0; ex<BOARD_X; ex++) {
						distance[ey][ex] = Integer.MAX_VALUE;
						if (end[ey][ex] == target) {
							tx = ex;
							ty = ey;
						}
					}
				}
				// 壁よけ
				if (!(tx == -1 && ty == -1)) {
					distance[ty][tx] = 0;
					setupdistance(distance, tx, ty);
					distanceMap.put(target, distance);
				}
			}
		}
		
		public int getSearchDistance() {
			int d = 0;
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					char c = board[y][x];
					if (c != BLOCK && c != SPACE) {
						d += distanceMap.get(c)[y][x];
					}
				}
			}
			return d;
		}
		
		public int getNextSearchDistance(Point current, Point next) {
			int d = 0;
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					char c = board[y][x];
					if (x == current.x && y == current.y) {
						d += distanceMap.get(board[next.y][next.x])[y][x];
					} else if (x == next.x && y == next.y) {
					} else {
						if (c != BLOCK) {
							d += distanceMap.get(c)[y][x];
						}
					}
				}
			}
			return d;
		}
		public Point search(char c) {
			for (int i=0; i<BOARD_Y; i++) {
				for (int j=0; j<BOARD_X; j++) {
					if (board[i][j] == c) {
						return new Point(j, i);
					}
				}
			}
			return null;
		}
		
		public boolean isBlock(int x, int y) {
			return board[y][x] == BLOCK;
		}
		@Override
		public boolean equals(Object obj) {
			char[][] check = ((Board) obj).board;
			for (int i=0, il=board.length; i<il; i++) {
				for (int j=0, jl=board[i].length; j<jl; j++) {
					if (board[i][j] != check[i][j]) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (char[] cs : board) {
				for (char c : cs) {
					sb.append(c);
				}
				sb.append("\r\n");
			}
			return sb.toString();
		}
		public String toNextString(Point current, Point next) {
			StringBuilder sb = new StringBuilder();
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					if (x == current.x && y == current.y) {
						sb.append(board[next.y][next.x]);
					} else if (x == next.x && y == next.y) {
						sb.append(board[current.y][current.x]);
					} else {
						sb.append(board[y][x]);
					}
				}
			}
			return sb.toString();
		}
		public String toEndString() {
			StringBuilder sb = new StringBuilder();
			for (char[] cs : end) {
				for (char c : cs) {
					sb.append(c);
				}
				sb.append("\r\n");
			}
			return sb.toString();
		}
		public String toDistanceString() {
			StringBuilder sb = new StringBuilder();
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					char c = board[y][x];
					if (c != BLOCK) {
						sb.append(distanceMap.get(c)[y][x]);
					} else {
						sb.append(0);
					}
				}
				sb.append("\r\n");
			}
			return sb.toString();
		}
		
		public boolean isEnd() {
			for (int y=0; y<BOARD_Y; y++) {
				for (int x=0; x<BOARD_X; x++) {
					if (board[y][x] != end[y][x]) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	class SpacePoint extends Point {
		private static final long serialVersionUID = 1L;
		String direction;
		String backdirection;
		public SpacePoint(int x, int y, String direction, String backdirection) {
			super(x, y);
			this.direction = direction;
			this.backdirection = backdirection;
		}
	}
	
	static class Settings {
		int LX;
		int RX;
		int UX;
		int DX;
		int NUM;
		public Settings(String line, String line2) {
			String[] tokens = line.split(" ");
			LX = Integer.parseInt(tokens[0]);
			RX = Integer.parseInt(tokens[1]);
			UX = Integer.parseInt(tokens[2]);
			DX = Integer.parseInt(tokens[3]);
			NUM = Integer.parseInt(line2);
		}
	}
	class AddArrayList<E> extends ArrayList<E> {
		private static final long serialVersionUID = 1L;
		@Override
		public E set(int index, E element) {
			if (size() <= index) {
				for (int i=size(); i<=index; i++) {
					add(null);
				}
			}
			return super.set(index, element);
		}
	}
}
