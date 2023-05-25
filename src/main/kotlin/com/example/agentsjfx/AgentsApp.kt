package com.example.agentsjfx

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.embed.swing.SwingNode
import javafx.scene.layout.BorderPane
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.jfx.DefaultPlotPanelJfx
import org.jetbrains.letsPlot.geom.geomDensity
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import javax.swing.JPanel
import kotlin.concurrent.thread


class AgentsApp : Application() {
    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(AgentsApp::class.java.getResource("main-view.fxml"))
        val root = fxmlLoader.load<BorderPane>()

        stage.title = "Lets-Plot in JavaFX Application Demo"

        val plotSwingPanel = createPlotPanel();

        val swingNode = SwingNode()
        swingNode.content = plotSwingPanel

        root.center = swingNode
//        root.children.add(vbox)
//        root.children.add(swingNode)
        stage.scene = Scene(root, 1024.0, 768.0)

        stage.show()

        thread {
            Thread.sleep(3000)
            Platform.runLater {
                val plot = plotSwingPanel
            }
            Thread.sleep(3000)
            Platform.runLater {
                root.center = swingNode
            }
        }
    }

    companion object {
        fun createPlotPanel(): JPanel {

//            // Make sure JavaFX event thread won't get killed after JFXPanel is destroyed.
//            Platform.setImplicitExit(false)

            // Density plot
            val rand = java.util.Random()
            val n = 200
            val data = mapOf<String, Any>(
                "x" to List(n) { rand.nextGaussian() }
            )

            val plot = letsPlot(data) + geomDensity(
                color = "dark-green",
                fill = "green",
                alpha = .3,
                size = 2.0
            ) { x = "x" }


            val rawSpec = plot.toSpec()
            val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

            return DefaultPlotPanelJfx(
                processedSpec = processedSpec,
                preserveAspectRatio = false,
                preferredSizeFromPlot = false,
                repaintDelay = 10,
            ) { messages ->
                for (message in messages) {
                    println("[Example App] $message")
                }
            }
        }
    }

}

fun main() {
    Application.launch(AgentsApp::class.java)
}