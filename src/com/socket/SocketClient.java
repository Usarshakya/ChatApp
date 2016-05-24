package com.socket;

import com.ui.ChatFrame;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

public class SocketClient implements Runnable{
    
    public int port;
    public String serverAddr;
    public Socket socket;
    public ChatFrame ui;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    public History hist;
    public SocketClient(ChatFrame frame) throws IOException{
         
        ui = frame; this.serverAddr = ui.serverAddr; this.port = ui.port;
        socket = new Socket(InetAddress.getByName(serverAddr), port);
            
        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());
        
        hist = ui.hist;
    }

    @Override
    public void run() {
        boolean keepRunning = true;
        while(keepRunning){
            try {
                Message msg = (Message) In.readObject();
                System.out.println("Incoming : "+msg.toString());
                
                if(msg.type.equals("message")&&!msg.sender.equalsIgnoreCase("server")){
                    
                    if((msg.recipient.equals(ui.username)||msg.sender.equals(ui.username)) && !(msg.recipient.equals("All")||msg.sender.equals("All"))) {

                        String index=null;
                        if(ui.chatwinindexlist.get(msg.recipient)==null && ui.chatwinindexlist.get(msg.sender)==null)
                        {
                            JTextArea name = new JTextArea();
                            name.setEditable(false);
                            if(msg.recipient.equals(ui.username))
                            {
                                ui.chatwinindexlist.put(msg.sender, ui.chatpane.indexOfComponent(name)+"");                        
                                ui.chatwinlist.put(ui.chatpane.indexOfComponent(name)+"", name);
                               ui.chatpane.add(name,msg.sender);
                               
                               
                            }
                            else
                            {
                                ui.chatwinindexlist.put(msg.recipient, ui.chatpane.indexOfComponent(name)+"");                        
                                ui.chatwinlist.put(ui.chatpane.indexOfComponent(name)+"", name);
                                 ui.chatpane.add(name,msg.recipient);
                                //  ui.addCloseableTab(ui.chatpane, msg.recipient, name);
                            }
                        }
                        if(msg.recipient.equals(ui.username))
                        {
                            index=(String)ui.chatwinindexlist.get(msg.sender);
                        }
                        else
                        {
                            index=(String)ui.chatwinindexlist.get(msg.recipient);
                        }
                        ((JTextArea)ui.chatwinlist.get(index)).show();
                        ((JTextArea)ui.chatwinlist.get(index)).append("["+msg.sender+"-->] : " + msg.content + "\n");
                    }
                    else{
                        ui.jTextArea1.append("["+ msg.sender+"-->] : " + msg.content + "\n");
                    }
                                            
                    if(!msg.content.equals(".bye") && !msg.sender.equals(ui.username)){
                        String msgTime = (new Date()).toString();
                        
                        try{
                            hist.addMessage(msg, msgTime);
                            DefaultTableModel table = (DefaultTableModel) ui.historyFrame.jTable1.getModel();
                            table.addRow(new Object[]{msg.sender, msg.content, "Me", msgTime});
                        }
                        catch(Exception ex){}  
                    }
                }
                else if(msg.type.equals("login")){
                    if(msg.content.equals("TRUE")){
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);                        
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                         ui.jButton12.setEnabled(true); ui.jButton17.setEnabled(true);
                         ui.jButton8.setEnabled(true);
                        ui.servermes.append("Login Successful\n");
                        ui.servermes.setEnabled(false); ui.jPasswordField1.setEnabled(false);
                        ui.jPanel2.hide();
                        ui.jPanel1.show();
//                        ui.jPanel4.show();
                        ui.jLabel6.setText("Hello, "+ui.jTextField3.getText()+"!");
                        ui.jLabel6.show();
                    }
                    else{
                       JOptionPane.showMessageDialog(ui, "Login Failed");
                    }
                }
                else if(msg.type.equals("test")){
                    ui.jButton1.setEnabled(false);
                    ui.jButton2.setEnabled(true); ui.jButton3.setEnabled(true);
                    ui.jTextField3.setEnabled(true); ui.jPasswordField1.setEnabled(true);
                    ui.jTextField1.setEditable(false); ui.jTextField2.setEditable(false);
//                    ui.jButton7.setEnabled(true);
                }
                else if(msg.type.equals("newuser")){
                    if(!msg.content.equals(ui.username)){
                        boolean exists = false;
                        for(int i = 0; i < ui.model.getSize(); i++){
                            if(ui.model.getElementAt(i).equals(msg.content)){
                                exists = true; break;
                            }
                        }
                        if(!exists){ ui.model.addElement(msg.content); }
                    }
                }
                else if(msg.type.equals("signup")){
                    if(msg.content.equals("TRUE")){
                        ui.jButton2.setEnabled(false); ui.jButton3.setEnabled(false);
                        ui.jButton4.setEnabled(true); ui.jButton5.setEnabled(true);
                        ui.servermes.append("Singup Successful\n");
                    }
                    else{
                        ui.servermes.append("Signup Failed\n");
                    }
                }
                else if(msg.type.equals("signout")){
                    if(msg.content.equals(ui.username)){
                        ui.servermes.append("Bye\n");
                        ui.jButton1.setEnabled(true); ui.jButton4.setEnabled(false); 
                        ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                        
                        for(int i = 1; i < ui.model.size(); i++){
                            ui.model.removeElementAt(i);
                        }
                        
                        ui.clientThread.stop();
                    }
                    else{
                        ui.model.removeElement(msg.content);
                        ui.servermes.append(msg.content +" has signed out\n");
                    }
                }
                else if(msg.type.equals("upload_req")){
                    
                    if(JOptionPane.showConfirmDialog(ui, ("Accept '"+msg.content+"' from "+msg.sender+" ?")) == 0){
                        
                        JFileChooser jf = new JFileChooser();
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);
                       
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION){
                            Download dwn = new Download(saveTo, ui);
                            Thread t = new Thread(dwn);
                            t.start();
                            //send(new Message("upload_res", (""+InetAddress.getLocalHost().getHostAddress()), (""+dwn.port), msg.sender));
                            send(new Message("upload_res", ui.username, (""+dwn.port), msg.sender));
                        }
                        else{
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }
                    }
                    else{
                        send(new Message("upload_res", ui.username, "NO", msg.sender));
                    }
                }
                else if(msg.type.equals("upload_res")){
                    if(!msg.content.equals("NO")){
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;
                        
                        ui.jButton5.setEnabled(false); ui.jButton6.setEnabled(false);
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                    }
                    else{
                        ui.servermes.append(msg.sender+" rejected file request\n");
                    }
                }
                else{
                    ui.servermes.append("Unknown message type\n");
                }
            }
            catch(Exception ex) {
                keepRunning = false;
                ui.servermes.append("Connection Failure\n");
                JOptionPane.showMessageDialog(ui, "Conection Failure");
                ui.jButton1.setEnabled(true); ui.jTextField1.setEditable(true); ui.jTextField2.setEditable(true);
                ui.jButton4.setEnabled(false); ui.jButton5.setEnabled(false); ui.jButton5.setEnabled(false);
                
                for(int i = 1; i < ui.model.size(); i++){
                    ui.model.removeElementAt(i);
                }
                
                ui.clientThread.stop();
                
                System.out.println("Exception SocketClient run()");
                ex.printStackTrace();
            }
        }
    }
    
    public void send(Message msg){
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
            
            if(msg.type.equals("message") && !msg.content.equals(".bye")){
                String msgTime = (new Date()).toString();
                try{
                    hist.addMessage(msg, msgTime);               
                    DefaultTableModel table = (DefaultTableModel) ui.historyFrame.jTable1.getModel();
                    table.addRow(new Object[]{"Me", msg.content, msg.recipient, msgTime});
                }
                catch(Exception ex){}
            }
        } 
        catch (IOException ex) {
            System.out.println("Exception SocketClient send()");
        }
    }
    
    public void closeThread(Thread t){
        t = null;
    }
}
