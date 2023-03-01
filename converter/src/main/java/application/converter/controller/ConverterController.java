package application.converter.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.core.util.FileUtils;
import org.jodconverter.local.LocalConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/converter")
@CrossOrigin("*")
public class ConverterController {

    private final OfficeManager officeManager;

    public ConverterController(OfficeManager officeManager) {
        super();
        this.officeManager = officeManager;
    }

    //Este metodo es el unico de la clase y el que se encarga de la conversión del archivo ODT a los formatos de salida DOCX y PDF
    //recibe como parametros una variable inputFile de tipo MultiPartFile, la cual es el archivo ODT, y de segundo parametro
    //una variable de tipo String llamada target, que especifica el formato de conversión, cuyo valor puede ser DOCX o PDF
    @PostMapping()
    public ResponseEntity<Object> convert(@RequestParam("file") final MultipartFile inputFile, @RequestParam("targetFormat") final String target) {

        if (inputFile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            //Declara e inicializa la variable targetFormat de tipo DocumentFormat que especifica el formato al cual sera 
            //convertido el archivo ODT, el cual es el formato DOCX
            DocumentFormat targetFormat = DefaultDocumentFormatRegistry.DOCX;

            //Esta condicional verifica si el parametro target es "PDF", si lo es, el formato de targetFormat pasa a ser PDF
            //y si no lo es, entonces el formato permanece como DOCX
            if(target.equalsIgnoreCase("PDF")){
                targetFormat = DefaultDocumentFormatRegistry.PDF;
            }

            //Declara e inicializa la variable converter de tipo DocumentConverter, que contiene el metodo convert()
            //el cual toma como parametros la variable inputFile y la variable targetFormat 
            final DocumentConverter converter = LocalConverter.builder().officeManager(officeManager).build();

            //Convierte el documento ODT a DOCX o PDF y luego lo transforma en un byteArrayOutputStream
            converter.convert(inputFile.getInputStream()).to(byteArrayOutputStream).as(targetFormat).execute();

            String targetFilename = FileUtils.getBaseName(inputFile.getOriginalFilename()) + "."
                    + targetFormat.getExtension();

            final HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.parseMediaType(targetFormat.getMediaType()));

            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + targetFilename);

            headers.setContentType(MediaType.parseMediaType(targetFormat.getMediaType()));

            //Esta es la respuesta que el backend envia a la aplicación o interfaz principal, la cual se encargara 
            //de darle un nombre al archivo, escribirle la terminación .odt o .pdf y almacenarla en el sistema de archivos
            return ResponseEntity.ok().headers(headers).body(byteArrayOutputStream.toByteArray());

        } catch (OfficeException | IOException e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

}
