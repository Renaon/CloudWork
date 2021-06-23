package com.polozov.cloudstorage.lesson02;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTelnetServer {
    private static final String LS_COMMAND = "\tls     view all files from current directory";
    private static final String MKDIR_COMMAND = "\tmkdir  view all files from current directory";
    private static final String TOUCH_COMMAND = "\t touch create file";
    private static final String CD_COMMAND = "\t cd go to path";
    private static final String RM_COMMAND = "\t rm delete file or directory";
    private static final String COPY_COMMAND = "\t copy copy file/directory";
    private static final String CHANGENICK = "\t changenick change your nickname";

    private String LOGIN;
    private String ROOT_PATH = "server" + File.separator;

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<SocketAddress, String> clients = new HashMap<>();

    public NioTelnetServer() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap("Hello user!\n".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info".getBytes(StandardCharsets.UTF_8)));
    }


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);

        if (readBytes < 0) {
            channel.close();
            return;
        } else  if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        // TODO: 21.06.2021
        // touch (filename) - создание файла
        // mkdir (dirname) - создание директории
        // cd (path | ~ | ..) - изменение текущего положения
        // rm (filename / dirname) - удаление файла / директории
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя

        // добавить имя клиента

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n"), selector, client);
            }
        }
    }
    private void mkdir(String wish){
        Path path = Paths.get(ROOT_PATH + wish);
        try{
            Path newPath = Files.createDirectory(path);
        } catch(FileAlreadyExistsException e) {
            System.out.println("File already exists");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String rm(String way){
        Path path = Paths.get(ROOT_PATH + way);
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("delete file: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("delete dir: " + dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch(IOException e){
            e.printStackTrace();
        }
        return "files deleted";
    }

    private Path cd(String moving){
        return Paths.get(ROOT_PATH + moving);
    }

    public String changenick(String LOGIN) {
        this.LOGIN = LOGIN;
        this.ROOT_PATH = LOGIN + File.separator;
        return LOGIN;
    }

    public String getLOGIN(){
        return this.LOGIN;
    }

    private String touch(String filename){
        //создадим файл
        Path path = Paths.get(this.ROOT_PATH + File.separator + filename);
        if (Files.exists(path)){
            System.out.println("Файл уже существует");
        }else{
            try {
                Files.createFile(path);
            }catch(IOException e){ e.printStackTrace();}
        }
        return "file is created";
    }

    private void cat(String filename){

    }

    private String copy(String src, String target){
        FileChannel srcCh = null;
        FileChannel tch = null;
        try{
            srcCh = new FileInputStream(new File(src)).getChannel();
            tch = new FileOutputStream(new File(target)).getChannel();
            tch.transferFrom(srcCh, 0, srcCh.size());

            srcCh.close();
            tch.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "copied";
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private String getFilesList() {
        String[] servers = new File("server").list();
        return String.join(" ", servers);
    }

    public static void main(String[] args) throws Exception {
        new NioTelnetServer();
    }
}
