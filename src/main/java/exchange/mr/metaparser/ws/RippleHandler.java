package exchange.mr.metaparser.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RippleHandler extends TextWebSocketHandler{
	
  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
      session.sendMessage(new TextMessage("You are now connected to the server. Your session is " + session.getId()));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
      System.out.println("Message received: " + textMessage.getPayload());
  }

}
