//package com.turingSecApp.turingSec.service.socket;
//
//import com.corundumstudio.socketio.SocketIOServer;
//import com.corundumstudio.socketio.listener.ConnectListener;
//import com.corundumstudio.socketio.listener.DataListener;
//import com.corundumstudio.socketio.listener.DisconnectListener;
//import com.turingSecApp.turingSec.exception.custom.ResourceNotFoundException;
//import com.turingSecApp.turingSec.model.entities.message.BaseMessageInReport;
//import com.turingSecApp.turingSec.model.entities.message.StringMessageInReport;
//import com.turingSecApp.turingSec.model.entities.report.Report;
//import com.turingSecApp.turingSec.model.repository.report.ReportsRepository;
//import com.turingSecApp.turingSec.model.repository.reportMessage.BaseMessageInReportRepository;
//import com.turingSecApp.turingSec.model.repository.reportMessage.StringMessageInReportRepository;
//import com.turingSecApp.turingSec.payload.message.StringMessageInReportPayload;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//@Service
//@Slf4j
//public class SocketService {
//    private final ReportsRepository reportsRepository;
//    private final BaseMessageInReportRepository baseMessageInReportRepository;
//    private final StringMessageInReportRepository stringMessageInReportRepository;
//
//    private SocketIOServer socketIOServer;
//
//    public SocketService(ReportsRepository reportsRepository, BaseMessageInReportRepository baseMessageInReportRepository, StringMessageInReportRepository stringMessageInReportRepository, SocketIOServer socketIOServer) {
//        // Inject other class
//        this.reportsRepository = reportsRepository;
//        this.baseMessageInReportRepository = baseMessageInReportRepository;
//        this.stringMessageInReportRepository = stringMessageInReportRepository;
//
//
//        // Inject socketIOServer
//        this.socketIOServer = socketIOServer;
//        socketIOServer.addConnectListener(onConnected());
//        socketIOServer.addDisconnectListener(onDisconnected());
//
//
//
//        socketIOServer.addEventListener("send_message_str", StringMessageInReportPayload.class, onStrMessageReceived()); // base/parent class Message
//        //socketIOServer.addEventListener("send_message_file", FileMessageInReport.class, onFileMessageReceived());
//    }
//
//    @Transactional // for this anno we cannot set private
//    public DataListener<StringMessageInReportPayload> onStrMessageReceived() {
//        return (socketIOClient, data, ackSender) -> {
//
//            StringMessageInReport strMessage = new StringMessageInReport();
//            strMessage.setContent(data.getContent());
//            strMessage.setEdited(false); // Initialize false, set true when change and set updatedAt
//            strMessage.setCreatedAt(LocalDateTime.now());
//            strMessage.setReplied(data.isReplied());
//
//            // Set "BaseMessage" if it is not null
//            if(data.getReplyToMessageId() != null){
//                Optional<BaseMessageInReport> repliedMessageInReport = baseMessageInReportRepository.findById(data.getReplyToMessageId());
//                strMessage.setReplyTo(repliedMessageInReport.get());
//            }
//
//            // Set "Report"
//            Report reportOfMessage = findReportById(data.getReportId());
//            strMessage.setReport(reportOfMessage);
//
//
//            //
//
//            // Send message to the report's room
////            String room = socketIOClient.getHandshakeData().getSingleUrlParam("room"); // room as query ?room= uuid of msg's report
////            log.info(String.format("Room uuid: %s , report of room uuid: %s , must be equal",room, reportOfMessage.getRoom()));
//
//
//            socketIOClient.getNamespace().getAllClients().forEach(  x -> {
//                // if (!x.getSessionId().equals(socketIOClient.getSessionId())){
//                x.sendEvent("get_message", data);
//                //}
//            } );
////            socketIOClient.getNamespace().getRoomOperations(room).getClients().forEach(
////                    x -> {
//////                        if (!x.getSessionId().equals(socketIOClient.getSessionId())){
////                            x.sendEvent("get_message", data); // todo: change payload into actual entity
//////                        }
////                    }
////            );
//
//            // Save str message
//            StringMessageInReport savedMsg = stringMessageInReportRepository.save(strMessage);
//
//            //todo: validation in msg payload
//            //todo: find is hacker or company based on accessToken in message
//
//            // Log important details of message
//            log.info(String.format("Sent message id: %d ,Report id: %d , Role is %s , Report's user id: %d,Socket client id: %s -> msg: %s at %s",
//                    savedMsg.getId(),
//                    data.getReportId(),
//                    data.isHacker() ? "Hacker" : "Company",
//                    reportOfMessage.getUser().getId(),
////                        reportOfMessage.getBugBountyProgram().getCompany().getId(),
////                        reportOfMessage.getBugBountyProgram().getId(),
//                    socketIOClient.getSessionId().toString(),
//                    data.getContent(),
//                    LocalDateTime.now()));
//        };
//    }
//
//    private Report findReportById(Long reportId) {
//        Report report = null;
//        try {
//            report = reportsRepository.findById(reportId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Report not found with id: " + reportId));
//        } catch (ResourceNotFoundException e) {
//            // Handle the exception here, possibly by logging it or sending an error response
//            log.error("Resource not found: " + e.getMessage());
//
//        }
//        return report;
//    }
////    private DataListener<FileMessageInReport> onFileMessageReceived() {
////        return null;
////    }
//
//
//    //
//    private ConnectListener onConnected() {
//        return socketIOClient -> {
//            //todo: user info must be logged detailed
//            // log.info(String.format("User with username:%s started the socket"));
//            log.info(String.format("SocketID: %s connected!", socketIOClient.getSessionId().toString()));
//        };
//    }
//
//    private DisconnectListener onDisconnected() {
//        return socketIOClient -> {
//          log.info(String.format("SocketID: %s disconnected!", socketIOClient.getSessionId().toString()));
//        };
//    }
//
//
//}
