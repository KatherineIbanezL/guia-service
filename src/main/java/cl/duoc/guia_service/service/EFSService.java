package cl.duoc.guia_service.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class EFSService {

    // Ruta de montaje física del EFS dentro del servidor Linux de la EC2
    private static final String EFS_MOUNT_PATH = "/app/efs";

    public String guardarTemporalmente(Long guiaId, byte[] contenidoPdf) throws IOException {
        File directorio = new File(EFS_MOUNT_PATH);
        if (!directorio.exists()) {
            directorio.mkdirs();
        }

        String nombreArchivo = "guia_" + guiaId + ".pdf";
        Path rutaCompleta = Paths.get(EFS_MOUNT_PATH, nombreArchivo);
        
        // Persistencia temporal en el sistema de archivos EFS
        Files.write(rutaCompleta, contenidoPdf);
        
        return rutaCompleta.toString();
    }

    public byte[] leerDesdeEfs(String ruta) throws IOException {
        return Files.readAllBytes(Paths.get(ruta));
    }

    public void eliminarDeEfs(String ruta) throws IOException {
        Files.deleteIfExists(Paths.get(ruta));
    }
}