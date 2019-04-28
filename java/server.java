package exp3;

import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class server
{
	public static String write_file_path = "C:\\Users\\Administrator\\Desktop\\java_output.c";
	private static int server_port;
	private static int client_port = 7777;
	public static int file_size = 0;
	public static int max_file_size = 524288;
	public static int position = 0;
	public static int frame_count = 0;
	
	public  static void main(String[] args) throws IOException
	{
		//get port from config.properties file
		String config_file = "C:\\Users\\Administrator\\Desktop\\Java\\computer_networking\\src\\exp3\\config.properties";
		String server_port_string = properties_loader.get_properties(config_file, "port");
		server_port = Integer.parseInt(server_port_string);
		
		
		DatagramSocket datagram_socket = new DatagramSocket(server_port);
        InetAddress inet_address = InetAddress.getLocalHost();
		
        //receive file_size
        byte[] file_size_byte = new byte[4];
        DatagramPacket file_size_packet = new DatagramPacket(file_size_byte, file_size_byte.length, inet_address, client_port);
        datagram_socket.setSoTimeout(10000);    
        try 
        {
        	System.out.println("server is listening...");
        	datagram_socket.receive(file_size_packet);
        	file_size = (int)file_size_byte[0]*(256 * 256 * 256)
        			  + (int)file_size_byte[1]*(256 * 256)
        			  + (int)file_size_byte[2]*256
        			  + (int)file_size_byte[3];            
            System.out.println("file size is:"+file_size);
            
        }
        catch (SocketTimeoutException e) 
        {
        	System.out.println("didn't receive file_length, server time out..."); 
        }
        
        char write_str[] = new char[max_file_size];
        
        while(position < file_size)
        {
        	//get the frame from client
            byte[] recvd_str = new byte[6];
            DatagramPacket frame_packet = new DatagramPacket(recvd_str, recvd_str.length, inet_address, client_port);
            datagram_socket.setSoTimeout(10000);    
            try 
            {
            	System.out.println("\nserver start receiving...");
            	datagram_socket.receive(frame_packet);

            	//find error in recvd_str
            	if(find_error(recvd_str))
            	{
            		System.out.println("error frame received, please resend");
            		//send message=0 back to client
            		byte[] return_msg = new byte[1];
            		return_msg[0] = 0;
            		DatagramPacket return_packet = new DatagramPacket(return_msg, return_msg.length, inet_address, client_port);
            		datagram_socket.send(return_packet);
            		System.out.println("return message is:"+return_msg[0]);
            	}
            	else//no error is found,
            	{
            		System.out.println("frame "+frame_count+" received successfully");
            		write_str[position + 0] = (char)recvd_str[0];
                	write_str[position + 1] = (char)recvd_str[1];
                	write_str[position + 2] = (char)recvd_str[2];
                	write_str[position + 3] = (char)recvd_str[3];

                	position = position + 4;
                	frame_count++;
                	//send message=1 back to client
                	byte[] return_msg = new byte[1];
            		return_msg[0] = 1;
            		DatagramPacket return_packet = new DatagramPacket(return_msg, return_msg.length, inet_address, client_port);
            		datagram_socket.send(return_packet);
            		System.out.println("return message is:"+return_msg[0]);
            	}
            	
                
            }//try
            catch (SocketTimeoutException e) 
            { 
            	System.out.println("no frame received, server time out"); 
            	break;
            }
        }//while
		System.out.println("---------------Communication process is over--------------\n");
		
		//把末尾无用字符去掉
		char[] write_str2 = new char[file_size - 7];
		int i=0;
		for(; i < file_size - 7 ; i++)
		{
			write_str2[i] = write_str[i];
		}
		System.out.println(i);
		
		FileWriter file_writer = null;
		file_writer = new FileWriter(write_file_path);
		file_writer.write(write_str2);
		file_writer.close();
		
		datagram_socket.close();
		
	}//main
	
	public static boolean find_error(byte recvd_str[])
	{
		byte[] data_str = new byte[4];
		byte[] recvd_crc = new byte[2];
		data_str[0] = recvd_str[0];
		data_str[1] = recvd_str[1];
		data_str[2] = recvd_str[2];
		data_str[3] = recvd_str[3];
		recvd_crc[0] = recvd_str[4];
		recvd_crc[1] = recvd_str[5];
		byte[] correct_crc = get_crc_code(data_str);
		if((correct_crc[0]==recvd_crc[0]) && (correct_crc[1]==recvd_crc[1]))
			return false;
		else  return true;
	}
	
	public static byte[] get_crc_code(byte data_str[])
	{
		long divisor = 0;
		byte[] crc_code = new byte[4];
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
	
	
}