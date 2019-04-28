package exp3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.net.SocketTimeoutException;
import exp3.properties_loader;

public class client
{
	public static String read_file_path = "C:\\Users\\Administrator\\Desktop\\hello.c";
	public static byte str_file[];
	public static int client_port = 7777;
	//public static int server_port = 8888;
	//public static int filter_error = 10;
	//public static int filter_lost = 10;
	public static int server_port;
	public static int filter_error;
	public static int filter_lost;
	public static long file_size;
	public static byte[] crc_code = new byte[2];
	public static byte[] data_str = new byte[4];
	public static int max_file_size = 524288;
	public static byte[] return_msg = new byte[1];
	public static int next_frame_to_send = 0;
	public static int frame_count = 0;
	public static properties_loader loader; 

	
	public static void main(String[] args) throws IOException
	{
		String config_file = "C:\\Users\\Administrator\\Desktop\\Java\\computer_networking\\src\\exp3\\config.properties";
		String server_port_string = properties_loader.get_properties(config_file, "port");
		server_port = Integer.parseInt(server_port_string);
		
		String filter_lost_str = properties_loader.get_properties(config_file, "filter_lost");
		filter_lost = Integer.parseInt(filter_lost_str);
		System.out.println("filter_lost"+filter_lost);
		
		String filter_error_str = properties_loader.get_properties(config_file, "filter_error");
		filter_error = Integer.parseInt(filter_error_str);
		System.out.println("filter_error"+filter_error);
		
		str_file = read_file(read_file_path);
		int position = 0;
		byte[] send_buffer = new byte[6];
		
		DatagramSocket datagram_socket = new DatagramSocket(client_port);
	    DatagramPacket send_packet;
	    InetAddress inet_address = InetAddress.getLocalHost();
		
        //send file_size to server
        byte[] file_size_byte = new byte[4];
        file_size_byte[0] = (byte)(file_size / (256 * 256 * 256));
        file_size_byte[1] = (byte)(file_size / (256 * 256));
        file_size_byte[2] = (byte)(file_size / 256);
        file_size_byte[3] = (byte)(file_size % 256);
        send_packet = new DatagramPacket(file_size_byte, file_size_byte.length, inet_address, server_port);
        datagram_socket.send(send_packet);
        
		while(true)
		{
			if(position>=file_size)
				break;
	        data_str = get_data_str(position);
			send_buffer = get_send_str(data_str);
            datagram_socket.setSoTimeout(10000);
            
            //lost frame
            Random random_lost = new Random(filter_lost);
            if(random_lost.nextInt(filter_lost)<1)
            {
            	System.out.println("this frame is supposed to be lost\n");
            }
            else
            {
            	//send error frame
            	Random random_error = new Random(filter_error);
            	if(random_error.nextInt(filter_error)<1)
            	{
            		send_buffer = add_error(send_buffer);
            		send_packet = new DatagramPacket(send_buffer, send_buffer.length, inet_address, server_port);
            		System.out.println("this frame is supposed to be wrong\n");
            	}
            	else
            	{
            		//send a correct frame
                    try
                    {
                    	send_packet = new DatagramPacket(send_buffer, send_buffer.length, inet_address, server_port);
                    	datagram_socket.send(send_packet);
                    	System.out.println("send frame:"+frame_count);
                    }
                    catch (SocketTimeoutException e) 
                    { 
                    	System.out.println("client send frame failed, time out..."); 
                    }
            	}
            }
            
            
            
            //receive return message from server
            try 
            {
        		DatagramPacket return_packet = new DatagramPacket(return_msg, return_msg.length, inet_address, server_port);
            	datagram_socket.receive(return_packet);
            	System.out.println("return message :"+return_msg[0]);
            }
            catch (SocketTimeoutException e) 
            { 
            	System.out.println("client time out 88"); 
            }
            
            //process the return message
            if(return_msg[0]==0)
            {
            	next_frame_to_send = 0;
            	System.out.println("next_frame_to_send:"+next_frame_to_send);
            	System.out.println("resend current frame");
            }
            if(return_msg[0]==1)
            {
            	next_frame_to_send = 1;
            	position = position + 4;
            	frame_count++;
            	System.out.println("server receive successfully");
            	System.out.println("next_frame_to_send:"+next_frame_to_send);
            	System.out.println("send next frame...");
            }
            System.out.println("----------------------------------------\n");
            
            try
			{
				Thread.sleep(100);
			} 
            catch (InterruptedException e)
			{
				e.printStackTrace();
			}
            
            
		}//while
		
		datagram_socket.close();
	}//main
	
	public static byte[] read_file(String read_file_path)
	{	
		//File file=new File(filename);
		File file = new File(read_file_path);
		file_size = file.length();
		byte[] str_file = new byte[max_file_size];
	    Reader reader=null;
	    int i = 0;
	    try 
	    {
	    	System.out.println("以字符为单位读取文件内容，一次一个字节：");
	    	reader=new InputStreamReader(new FileInputStream(file));
	    	int temp;
	    	while ((temp = reader.read()) != -1) 
	    	{
	    		if (((byte)temp) != '\r') 
	    		{
	    			str_file[i] = (byte)temp;
	    			i++;
	    		}
	    	} 
	    	reader.close();
	    } 
	    catch (Exception e) 
	    {
	    	e.printStackTrace();
	    }
		return str_file;
	}
	
	public static byte[] get_data_str(int position)
	{
		byte[] data_str = new byte[4];
		data_str[0] = str_file[position + 0];
		data_str[1] = str_file[position + 1];
		data_str[2] = str_file[position + 2];
		data_str[3] = str_file[position + 3];
		return data_str;
	}

	public static byte[] get_crc_code(byte data_str[])
	{
		long divisor = 0;
		divisor = (long)Math.pow(2, 16) + (long)Math.pow(2, 12) + (long)Math.pow(2, 5) + 1;
		long d0 = data_str[0] * (long)Math.pow(2, 40);
		long d1 = data_str[1] * (long)Math.pow(2, 32);
		long d2 = data_str[2] * (long)Math.pow(2, 24);
		long d3 = data_str[3] * (long)Math.pow(2, 16);

		long data = 0;
		data = d0 + d1 + d2 + d3;
		int position = 31;
		long remainder = 0;
		remainder = data >> position;

		while (position != 0)
		{
			if ((remainder & 0x10000)!=0)
			{
				remainder = remainder ^ divisor;
			}
			position--;
			long next_position = (data >> position) % 2;
			remainder = remainder << 1;
			remainder += next_position;
		}

		crc_code[0] = (byte) ((remainder / 256) - 128);
		crc_code[1] = (byte) ((remainder % 256) - 128);
		return crc_code;
	}
	
	public static byte[] get_send_str(byte data_str[])
	{
		byte[] send_str = new byte[6];
		byte[] crc_code = get_crc_code(data_str);
		send_str[0] = data_str[0];
		send_str[1] = data_str[1];
		send_str[2] = data_str[2];
		send_str[3] = data_str[3];
		send_str[4] = crc_code[0];
		send_str[5] = crc_code[1];
		return send_str;
	}
	
	public static byte[] add_error(byte[] send_buffer)
	{
		byte[] error_buffer = new byte[6];
		error_buffer[0] = (byte) (send_buffer[0] + 1);
		error_buffer[1] = (byte) (send_buffer[1] + 1);
		error_buffer[2] = (byte) (send_buffer[2] + 1);
		error_buffer[3] = (byte) (send_buffer[3] + 1);
		error_buffer[4] = (byte) (send_buffer[4] + 1);
		error_buffer[5] = (byte) (send_buffer[5] + 1);
		return error_buffer;
	}

	
	
}