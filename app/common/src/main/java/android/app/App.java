package android.app;

import com.codename1.system.Lifecycle;
import com.codename1.ui.*;
import com.codename1.ui.layouts.*;
import java.io.IOException;
import java.io.OutputStream;

import com.codename1.ext.filechooser.FileChooser;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.MultipartRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;

public class App extends Lifecycle {

    //La variable targetFormat de tipo String hace referencía al formato que sera convertido el archivo ODT
    private String targetFormat;

    public void init(){
        //La variable targetFormat es inicializada con el valor DOCX por defecto
        targetFormat = "DOCX";
    }
 
    @Override
    public void runApp() {
        //El metodo runApp es similar al metodo start() del cíclo de vida de las Applets, dentro del metodo se declara
        //la variable panel de tipo Form, el cual es análogo al tipo JFrame de Swing, luego se crean dos variables de tipo
        //RadioButton, similares en función al elemento HTML radio button, con los valores "DOCX" y "PDF", luego se crea
        //un buton de tipo Button, es decir, un buton normal y corriente, el cual se encargara de llamar al metodo chooseDocument
        //que permite al usuario elegir de su sistema de archivos el documento ODT a convertir
        Form panel = new Form("ODT a DOCX/PDF", BoxLayout.y());
        RadioButton radioButtonOne = new RadioButton("DOCX");
        RadioButton radioButtonTwo = new RadioButton("PDF");
        new ButtonGroup(radioButtonOne, radioButtonTwo);
        radioButtonOne.setSelected(true);
        radioButtonOne.setUnselectAllowed(true);
        radioButtonTwo.setUnselectAllowed(true);
        Button openButton = new Button("Selecciona archivo ODT");
        panel.add(openButton);
        panel.add(radioButtonOne);
        panel.add(radioButtonTwo);
        openButton.addActionListener(e -> chooseDocument(panel));
        radioButtonOne.addActionListener(e -> targetFormat = "DOCX");
        radioButtonTwo.addActionListener(e -> targetFormat = "PDF");
        panel.show();
    }

    //Este metodo se encarga de abrir una ventana que permite al usuario escoger archivos de tipo ODT de su sistema
    private void chooseDocument(Form panel) {
        FileChooser.setOpenFilesInPlace(true);
        FileChooser.showOpenDialog(".odt", e -> {
            if (e != null && e.getSource() != null) {
                String file = (String) e.getSource();
                uploadFileToServer(file, panel);
                panel.revalidate();
                return;
            } else {
                panel.add("Ningun archivo fue seleccionado");
                panel.revalidate();
                return;
            }
        });
    }

    private void uploadFileToServer(String filePath, Form panel) {
        //Las dos siguientes líneas de código se encargan de establecer parametros para crear la conexión
        //con el backend de la aplicación con la variable request, al cual será enviado el archivo ODT seleccionado y se encargara de responder
        //con el archivo ODT ya convertido al formato de destino establecido en la variable targetFormat
        MultipartRequest request = new MultipartRequest();
        request.setUrl("https://localhost:8080/api/converter");

        try {
            request.addData("file", filePath, "application/vnd.oasis.opendocument.present");
            request.addArgument("targetFormat", targetFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Este metodo crea la conexión con el backend utilizando la variable request
        NetworkManager.getInstance().addToQueueAndWait(request);

        //Este metodo obtiene la respuesta del backend, respuesta que es el archivo ODT ya convertido al formato de destino
        //representado mediante una array de bytes
        byte[] data = request.getResponseData();

        if(data != null){
            panel.add("Documento convertido exitosamente");
            panel.revalidate();
            saveFile(data);
        } else {
            panel.add("Hubo un error en la conversión del documento");
            panel.revalidate();
        }
    }

    //Este metodo se encarga de escribir la array de bytes obtenida como respuesta del backend en el metodo
    //uploadFileToServer a un documento de formato DOCX o PDF con nombre random para luego almacenarlo en el sistema de archivos
    private void saveFile(byte[] data) {
        FileSystemStorage systemStorage = FileSystemStorage.getInstance();
        String fileName = systemStorage.getAppHomePath() + Util.getUUID() + "." + targetFormat.toLowerCase();

        try (OutputStream outputStream = systemStorage.openOutputStream(fileName)) {
            outputStream.write(data);
            Util.cleanup(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
