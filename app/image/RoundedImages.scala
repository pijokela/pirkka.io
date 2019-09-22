package image

import java.awt.image.BufferedImage
import java.awt.geom.RoundRectangle2D
import java.awt.Color
import java.awt.RenderingHints
import java.awt.AlphaComposite
import java.awt.geom.AffineTransform
import java.io.File
import javax.imageio.ImageIO
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.StandardOpenOption
import java.nio.file.Path
import scala.concurrent.Future

object RoundedImages {
  
  def main(argv: Array[String]): Unit = {
    val fileName = "DSC_0006.jpg"
    
    val path = Paths.get("/tmp/" + fileName)
    val data = Files.readAllBytes(path)
    
    val result = createRoundedIcon(data)
    val pathOut = Paths.get("/tmp/" + fileName.subSequence(0, fileName.length()-4) + "-icon.png")
    Files.write(pathOut, result, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
  }
  
  def multipartFormdataToIcon(path: Path): Future[Array[Byte]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      val data = Files.readAllBytes(path)
      createRoundedIcon(data)
    }
  }
  
  def createRoundedIcon(bytesIn: Array[Byte]): Array[Byte] = {
    println("Creating website icon")
    val original = ImageIO.read(new ByteArrayInputStream(bytesIn))
    
    val targetWidth = 300
    val originalSmallSize = Math.min(original.getHeight, original.getWidth)
    val scalingFactor = targetWidth * 1.0 / originalSmallSize
    
    println(s"- Scaling to $targetWidth with scaling factor $scalingFactor.")
      
    val scaled = scale(original, targetWidth, targetWidth, scalingFactor, scalingFactor)
    
    val rounded = makeRoundedCorner(scaled, targetWidth)
    
    //val rounded = makeRoundedCorner(original, 20);
    // ImageIO.write(rounded, "png", new File("/tmp/icon.rounded.png"));
    val bytesOut = new ByteArrayOutputStream()
    ImageIO.write(rounded, "png", bytesOut)
    println("- Done")
    bytesOut.toByteArray()
  }

  /**
   * https://stackoverflow.com/questions/15558202/how-to-resize-image-in-java
   * 
   * scale image
   * 
   * @param sbi image to scale
   * @param dWidth width of destination image
   * @param dHeight height of destination image
   * @param fWidth x-factor for transformation / scaling
   * @param fHeight y-factor for transformation / scaling
   * @return scaled image
   */
  def scale(sbi: BufferedImage, dWidth: Int, dHeight: Int, fWidth: Double, fHeight: Double): BufferedImage = {
      val dbi = new BufferedImage(dWidth, dHeight, BufferedImage.TYPE_INT_ARGB);
      val g2 = dbi.createGraphics();
      val at = AffineTransform.getScaleInstance(fWidth, fHeight);
      g2.drawRenderedImage(sbi, at);
      dbi
  }
  
  /**
   * https://stackoverflow.com/questions/7603400/how-to-make-a-rounded-corner-image-in-java
   */
  def makeRoundedCorner(image: BufferedImage, cornerRadius: Int): BufferedImage = {
    val w = image.getWidth();
    val h = image.getHeight();
    val output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

    val g2 = output.createGraphics();

    // This is what we want, but it only does hard-clipping, i.e. aliasing
    // g2.setClip(new RoundRectangle2D ...)

    // so instead fake soft-clipping by first drawing the desired clip shape
    // in fully opaque white with antialiasing enabled...
    g2.setComposite(AlphaComposite.Src);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(Color.WHITE);
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

    // ... then compositing the image on top,
    // using the white shape from above as alpha source
    g2.setComposite(AlphaComposite.SrcAtop);
    g2.drawImage(image, 0, 0, null);

    g2.dispose();

    return output;
  }
}
