package com.vpmcoe.clubconnect.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.vpmcoe.clubconnect.model.EventRegistration;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class EventPassPdfService {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] createPass(EventRegistration registration) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5, 32, 32, 32, 32);
            PdfWriter.getInstance(document, output);
            document.open();

            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.decode("#0d6efd"));
            Font heading = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            document.add(new Paragraph("VPM COLLEGE OF ENGINEERING", heading));
            document.add(new Paragraph("CLUBCONNECT EVENT PASS", title));
            document.add(new Paragraph("\n"));

            addLine(document, "EVENT", registration.getEvent().getTitle(), heading, body);
            addLine(document, "ATTENDEE", registration.getStudent().getFullName(), heading, body);
            addLine(document, "ROLL NUMBER", registration.getStudent().getPrn(), heading, body);
            addLine(document, "BRANCH", registration.getStudent().getDepartment(), heading, body);
            addLine(document, "YEAR", registration.getStudent().getAcademicYear(), heading, body);
            addLine(document, "DATE & TIME", registration.getEvent().getEventDate().format(DATE_TIME), heading, body);
            addLine(document, "VENUE", registration.getEvent().getVenue(), heading, body);
            addLine(document, "MODE", registration.getEvent().getMode().name(), heading, body);
            addLine(document, "ORGANIZED BY", registration.getEvent().getClub().getName(), heading, body);
            addLine(document, "PASS ID", registration.getTicketNumber(), heading, body);
            addLine(document, "STATUS", "REGISTERED", heading, body);

            Image qr = Image.getInstance(createQrCode(registration.getTicketNumber(), 180));
            qr.setAlignment(Image.ALIGN_CENTER);
            document.add(new Paragraph("\n"));
            document.add(qr);
            Paragraph footer = new Paragraph("Present this pass at the event entrance.", body);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate event pass.", ex);
        }
    }

    public byte[] createQrCode(String ticketNumber, int size) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(ticketNumber, BarcodeFormat.QR_CODE, size, size);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate event pass barcode.", ex);
        }
    }

    private void addLine(Document document, String label, String value, Font heading, Font body)
            throws DocumentException {
        document.add(new Paragraph(label, heading));
        document.add(new Paragraph(value == null ? "—" : value, body));
        document.add(new Paragraph(" "));
    }
}
