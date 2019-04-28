package exp3;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class properties_loader
{
//	public static void main(String[] args) throws IOException
//	{
//		Properties properties = new Properties();
//		
//		// 使用InPutStream流读取properties文件
//		BufferedReader bufferedReader = new BufferedReader(new FileReader("\\config.properties"));
//		
//		properties.load(bufferedReader);
//		
//		// 获取key对应的value值
//		properties.getProperty(key);
//	}
	
	public static String get_properties(String filePath, String keyWord)
	{
		Properties prop = new Properties();
		String value = null;
		try 
		{
			// 通过输入缓冲流进行读取配置文件
			InputStream InputStream = new BufferedInputStream(new FileInputStream(new File(filePath)));
			// 加载输入流
			prop.load(InputStream);
			// 根据关键字获取value值
			value = prop.getProperty(keyWord);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return value;
	}
	
}
