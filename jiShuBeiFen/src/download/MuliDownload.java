package download;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <pre>
 * The Class MuliDownload2.
 * 
 * Description:
 * 多线程下载：
 * 1.创建一个和服务器文件相同大小的文件
 * 2.开启若干个线程下载服务器资源
 * 3.每个线程下载相应的模块 
 * 
 * author: 时波波
 * 
 * Revision History:
 * <who>   <when>   <what>
 *        2014-4-25 create
 * 
 * </pre>
 */
public class MuliDownload {

	private static String PATH = "http://192.168.21.32:8080/download/setup.exe";
	private static int runningThreadCount = 0;
	private static int threadCount; // 总线程数

	public static String getFileName(String path) {
		return path.substring(path.lastIndexOf("/") + 1);
	}

	public static void main(String[] args) {
		try {
			URL url = new URL(PATH);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setRequestMethod("GET");
			int code = conn.getResponseCode();
			if (code == 200) {
				int length = conn.getContentLength();
				System.out.println("length=" + length);
				// 1.创建一个和服务器资源大小相同的文件
				RandomAccessFile raf = new RandomAccessFile(getFileName(PATH),
						"rwd");
				raf.setLength(length);

				// 2.开启若干个线程 下载
				threadCount = 3;
				int blocksize = length / threadCount;

				runningThreadCount = threadCount;

				for (int threadId = 1; threadId <= threadCount; threadId++) {
					// 开始下载 和 结束下载位置
					int startPos = (threadId - 1) * blocksize;
					int endPos = threadId * blocksize - 1;
					if (threadId == threadCount) {
						endPos = length;
					}
					System.out.println("threadId" + threadId + " : " + startPos
							+ "~" + endPos);
					new DownloadThread(threadId, startPos, endPos, PATH)
							.start();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class DownloadThread extends Thread {
		private int threadId;
		private int startPos;
		private int endPos;
		private String path;

		public DownloadThread(int threadId, int startPos, int endPos,
				String path) {
			this.threadId = threadId;
			this.startPos = startPos;
			this.endPos = endPos;
			this.path = path;
		}

		@Override
		public void run() {
			try {
				File file = new File(getFileName(path) + threadId + ".txt"); // 用来记录上次下载到什么地方
				if (file.exists() && file.length() > 0) {
					FileInputStream fis = new FileInputStream(file);
					BufferedReader br = new BufferedReader(
							new InputStreamReader(fis));
					String saveStartPos = br.readLine();
					if (saveStartPos != null && saveStartPos.length() > 0) {
						startPos = Integer.parseInt(saveStartPos); // 如果存在记录，把上次下载到的位置读取出来
					}
					fis.close();
				}

				URL url = new URL(path);
				HttpURLConnection conn = (HttpURLConnection) url
						.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(5000);
				conn.setRequestProperty("Range", "bytes=" + startPos + "-"
						+ endPos);
				System.out.println("threadId" + threadId + "开始下载 : " + startPos
						+ "~" + endPos);

				InputStream is = conn.getInputStream();
				RandomAccessFile raf = new RandomAccessFile(getFileName(path),
						"rwd");
				raf.seek(startPos);
				int len = 0;

				byte[] buffer = new byte[1024 * 1024];
				// byte[] buffer = new byte[1024];

				int total = 0; // 当前这次下载的数据大小
				while ((len = is.read(buffer)) != -1) {
					raf.write(buffer, 0, len);
					total += len; // 数据记录下来

					// 记录下次下载数据的开始位置
					RandomAccessFile raffile = new RandomAccessFile(
							getFileName(path) + threadId + ".txt", "rwd");
					String newStartPos = String.valueOf(startPos + total);
					raffile.write(newStartPos.getBytes());
					raffile.close();
				}
				is.close();
				raf.close();
				System.out.println("threadId" + threadId + "下载 完毕...");

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				// 线程执行的最后
				synchronized (MuliDownload.PATH) {
					runningThreadCount--;
					if (runningThreadCount == 0) {
						for (int i = 1; i <= threadCount; i++) {
							File file = new File(getFileName(path) + i + ".txt");
							System.out.println(file.delete());
						}
					}
				}
			}
		}
	}

}
