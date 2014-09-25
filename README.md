nioserver
=========

NioServer is a library for creating simple, yet flexible and scalable services.
It supplies infostructure for creating both servers and clients. In order to create
service one needs to implement class representing business logic and another one
defining state that should be saved between requests sent by a client. For more information
see comments ( or javadoc) for NioServer.java, ClientHelper.java. etc. NioServer provides
expiration managment of incoming connections thus preventing unnecessary resourec consumption.

Tests show low overhead imposed by the library - on pretty old core duo laptop with 2Gb memory
test for message size of 3000 bytes and 800 concurrent active clients ( and some more 
inactive ones) the throughput is some about rps. It seems that bottlenec is cheap gigabit switch
as  there is a free memory on system ( default jvm settings used) and load is far below 100%.
Note that underlying OS parameters (as quatas for number of concurrently open files) need to be changed
for server to work with large number of concurrent connections. So if you get 'resource unavailable'
kind of IOErrors please consult your OS documentation...


NioServer allows many configuration options through settings in NioServerInitializer.I tried to
provide as much tuning options as I could, please see code in the class for more information.
On other hand, default settings allow creation of the server in a very simple manner and without
excessive configuration...

LICENSE
=======

NioServer is writen by Boris Litvin. It is distributed under Lesser GPL license. See COPYING and COPYING.LESSER files 
for details.If you want to use the library, but for some reason can't because of license, please contact me, 
let's see what can be done.

HOW TO USE
==========

Probably best way to understand how to use the library is to see through test sources.

Simple scenario is going like this:

1. Decide what infomration should be memory persisted between invocations. Accordingly create class 
   that extends ClientContext. If the class has default constructor, you're done with this step. If
   you need to supply additional parameters to constructor, you have to create a factory implementing
   ClientContextFactory. Let's assume default constructor is good enough and you created class 
   MyClientContext  accomodating state corresponding to the connected client.

2. Write class handling your business logic. It should implement MessageProcessor interface. Once again
  if you need some non-default constructor initialization, create factory class implementing 
  MessageProcessorFactory interface. Let's assume you created MyMessageProcessor class with processData
  method. This method is called upon receiving message from a client. Parameter of the invocation is client
  context object corresponding to the client. The method is invoked by thread pool and the framework ensures
  proper handover of the object to the thread pool thread, so you don't need to worry about thread safety unless
  you specifically do threading stuff. To get the message use getRequest method of the client context object and 
  for replying use setReply. As you probably don't work with raw bytes, but with objects, you can use 
  ObjectEncoderDecoder helper class to convert objects to and from byte arrays.

3. On server side: create server by issuing ( for sake of example let's assume you want server on 12345 port)
     server= new NioServer<>(12345, MyClientContext.class, MyMessageProcessor.class,null);
   or if you can set properties of the application , you can define properties ( e.g. 
   command line parameters  like -Dorg.blitvin.nioserver.clientContextClass= ) 
     * org.blitvin.nioserver.clientContextClass= MyClassContext
     * org.blitvin.nioserver.messageProcessorClass=MyMessageProcessor
   you can use just 
     server = new NioServer<>(12345);

   You probably want to start new thread for the server

    Thread serverThread = new Thread(Server);
    serverThread.start();


4. On client side: assuming that server runs on myhost.mydomain.com
   LVClientHelper helper = new LVClientHelper("myhost.mydomain.com", 12345);

   to send messages and get responces you should use (assuming that server encodes object of class ReplyClass in
   the responce ) something like 
       try {
      	   ReplyClass reply = (ReplyClass) helper.sendObjectRequest(message);
        }
       catch(RemoteException ex) {
       // something wrong happen during business logic execution
       // so put here error handling code, for original exception use ex.getCause()
       }

     When you done, don't forget to close connection (well, actually, server manages expiration of client connections
    so if you do forget, or your code throws exception before you close connection, server takes care of this)
     helper.closeConnection();


5. If you need more control over server parameters see NioServerInitializer, set parameters as you need and use 
  NioServer(NioServerInitializer) constructor..
