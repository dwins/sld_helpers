package me.winslow.d.sldhelper
import scala.swing._
import javax.swing

object ActualWork {
  import xml._, transform._

  val colorNames = Set("fill", "stroke")
  def isColorNode(e: Elem): Boolean =
    (e.label == "CssParameter") && 
    (e.attributes.asAttrMap.get("name") exists (colorNames)) && 
    (e.child.forall { 
      case (_: xml.Text) | (_: xml.Comment) => true
      case _ => false
    })

  class AdjustSaturation(factor: Double) extends RewriteRule {
    import java.awt.Color.{ HSBtoRGB, RGBtoHSB }
    val ColorFormat = """(?:#|0x)?([0-9a-fA-F]{6})""".r

    def adjustColor(e: Elem): Seq[Node] = {
      val hex =
        try {
          val ColorFormat(hex) = e.text.trim
          hex
        } catch {
          case _ => sys.error("Text does not look like a color: " + e.text.trim)
        }
      val Seq(r, g, b) = hex.grouped(2).map { java.lang.Integer.parseInt(_, 16) } toSeq
      val Array(h, s, v) = RGBtoHSB(r, g, b, Array.ofDim[Float](3))
      val s_ = s * factor.toFloat
      val newRGB = HSBtoRGB(h, s_, v)
      e.copy(child = Text("#%06x".format(0xFFFFFF & newRGB)))
    }

    override def transform(node: Node): Seq[Node] =
      node match {
        case e: Elem if isColorNode(e) =>
          adjustColor(e)
        case n => n
      }
  }

  def adjust(doc: xml.Node, d: Double): xml.Node = {
    val tx = new RuleTransformer(new AdjustSaturation(d))
    tx(doc)
  }
}

object GUI extends SwingApplication {

  import GridBagPanel._, ActualWork.adjust

  class InputBox extends GridBagPanel {
    val description = new Label("Find all colors in an SLD file and adjust their saturation.")
    description.font = description.font.deriveFont(java.awt.Font.BOLD)
    val sldFileLabel = new Label("SLD File")
    val sldFileField = new TextField(30)
    val fileChooserLauncher = new Button("Open...")
    val adjustmentLabel = new Label("Adjust Saturation by Factor")
    val adjustmentField = new TextField("100%")
    val runButton = new Button("Apply Adjustments")

    private val fileChooser = new FileChooser()
    fileChooser.fileSelectionMode =
      FileChooser.SelectionMode.FilesOnly

    fileChooserLauncher.action = Action("Open...") {
      if (fileChooser.showOpenDialog(this) == FileChooser.Result.Approve)
        sldFileField.text = fileChooser.selectedFile.getAbsolutePath
    }
    

    border = new swing.border.EmptyBorder(3, 3, 3, 3)
    layout += (
      description -> new Constraints {
        grid = (0, 0)
        gridwidth = 3
      },
      sldFileLabel -> ((0, 1): Constraints),
      sldFileField -> ((1, 1): Constraints),
      fileChooserLauncher -> ((2, 1): Constraints),
      adjustmentLabel -> ((0, 2): Constraints),
      adjustmentField -> new Constraints {
        grid = (1, 2)
        gridwidth = 2
      },
      runButton -> new Constraints {
        grid = (1, 3)
        gridwidth = 2
      }
    )
  }

  class ProgressReporter(owner: Window) extends Dialog(owner) {
    modal = true
    title = "Updating..."
    
    val progress = new ProgressBar
    progress.label = "Applying changes to SLD..."
    // progress.min = 0
    // progress.max = 100
    progress.indeterminate = true

    contents = progress
  }
  
  def startup(args: Array[String]) {
    locally {
      import javax.swing.UIManager.{ getInstalledLookAndFeels, setLookAndFeel }
      import collection.JavaConversions._
      for (nimbus <- getInstalledLookAndFeels.find(_.getName == "Nimbus"))
        setLookAndFeel(nimbus.getClassName)
    }

    val frame = new MainFrame
    val input = new InputBox

    locally {
      input.runButton.action = Action("Apply Adjustments") {
        try {
          val format = new java.text.DecimalFormat("0.#%")
          require(
            input.sldFileField.text.nonEmpty &&
            new java.io.File(input.sldFileField.text).isFile,
            "Can't find file: " + input.sldFileField.text
          )
          val factor = format.parse(input.adjustmentField.text).doubleValue
          require(0d <= factor && factor <= 2d,
            "Adjustment factor (" + input.adjustmentField.text + ") must be between 0 and 100%"
          )
          val doc = xml.XML.loadFile(input.sldFileField.text)
          val adjusted = adjust(doc, factor)
          xml.XML.save(input.sldFileField.text, adjusted)
        } catch {
          case ex =>
            Dialog.showMessage(
              parent = input,
              title = "Oops",
              message = ex.getMessage,
              messageType = Dialog.Message.Error
            )
        }
      }
    }

    locally { import frame._
      title = "SLD Quickfix"
      visible = true
      contents = {
        val tabs = new TabbedPane
        tabs.pages += new TabbedPane.Page("Saturation", input)
        tabs
      }
    }
  }
}
