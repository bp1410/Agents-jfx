package com.example.agentsjfx

import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.hStep
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.providerPolicy
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.randExpoD
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.reporterPolicy
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.sBiasP
import com.example.agentsjfx.AgentsApp.Companion.Policy.Companion.sBiasR
import javafx.application.Application
import javafx.application.Platform
import javafx.embed.swing.SwingNode
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import jetbrains.datalore.plot.MonolithicCommon
import jetbrains.datalore.vis.swing.jfx.DefaultPlotPanelJfx
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.intern.Plot
import org.jetbrains.letsPlot.intern.toSpec
import org.jetbrains.letsPlot.letsPlot
import java.util.*
import kotlin.concurrent.thread
import kotlin.random.Random


class AgentsApp : Application() {
    
    override fun start(stage: Stage) {
        stage.title = "Lets-Plot in JavaFX Application Demo"

//        root.children.add(vbox)
//        root.children.add(swingNode)
        stage.scene = Scene(root, 1024.0, 768.0)

        stage.show()

//        thread {
//            Thread.sleep(3000)
//            Platform.runLater {
//                val plot = plotSwingPanel
//            }
//            Thread.sleep(3000)
//            Platform.runLater {
//                root.center = swingNode
//            }
//        }
    }

    companion object {
        val fxmlLoader = FXMLLoader(AgentsApp::class.java.getResource("main-view.fxml"))
        val root = fxmlLoader.load<BorderPane>()

        val data = LinkedList<Cycle>()
        val random = Random(100)
        class Params(
            val N: Int,
            val S: Int,
            val NC: Int,
            val KMin: Int,
            val KMax: Int,
            val ExpoA: Double,
            val ExpoG: Double,
            val V0: Double,
            val X: Double,
            val Y: Double,
            val Z: Double,
            var sAgent: IntArray?
        ) {
            init{
                require(sAgent == null || sAgent!!.size == N){"Size of sAgent array must be N or null. 1 for strategic."}
                sAgent?:run {
                    sAgent = IntArray(N)
                    var inSA = 0
                    var rn = 0
                    while (inSA < S) {
                        rn = random.nextInt(0, N)
                        if (sAgent!![rn]== 0) {    // !! not null asserted call
                            sAgent!![rn] = 1
                            inSA += 1
                        }
                    }
                }
                println(sAgent!!.joinToString(separator = "|", prefix = "sAgent(${sAgent!!.count(){it==1}}):[", postfix = "]"))
            }
            override fun toString(): String {
                return "Params(N=$N, " +
                        "S=$S, " +
                        "NC=$NC, " +
                        "KMin=$KMin, " +
                        "KMax=$KMax, " +
                        "ExpoA=${"%.2f".format(ExpoA)}, " +
                        "ExpoG=${"%.2f".format(ExpoG)}, " +
                        "V0=${"%.2f".format(V0)}, " +
                        "X=${"%.2f".format(X)}, " +
                        "Y=${"%.2f".format(Y)}, " +
                        "Z=${"%.2f".format(Z)})"
            }
        }

        class Cycle(
            val V: DoubleArray,
            val meanVs: Double,
            val meanVh: Double,
            val netOutflow: Double,
            val disc: BooleanArray
        )

        class Policy {
            companion object {
                fun hStep(v: Double, x: Double): Double { // 1(v>=1-x)
                    return if (v >= 1 - x) 1.0 else .0
                }
                fun sBiasP(y: Double, L: Double): Double{ // p
                    return minOf(y, L)
                }
                fun sBiasR(z: Double, L: Double): Double{ // r
                    return minOf(z, L)
                }
                fun providerPolicy(A: Double, p: Double): Double{ // Pij
                    return minOf(A, p)
                }
                fun reporterPolicy(G: Double, P: Double, r: Double): Double{ // Rij
                    return minOf(G*P, r)
                }
                fun randExpoD(expo: Double): Double{
                    return Math.pow(random.nextDouble(), 1.0/expo)
                }

            }
        }

//        from: https://github.com/alshan/lets-plot-mini-apps/issues/2
//        alshan commented on Sep 27, 2022
//        Ho @fmvin , this must be some trick with offscreen drawing of kind. Perhaps CardLayout can help.
//        Lets-Plot itself doesn't support incremental updates at the moment so, every time the data is updated a brand new plot object must be created.

        fun plot(series: List<Pair<String, List<Double>>>): Unit {

//            // Make sure JavaFX event thread won't get killed after JFXPanel is destroyed.
//            Platform.setImplicitExit(false)

//            // Density plot
//            val rand = java.util.Random()
//            val n = 200
//            val data = mapOf<String, Any>(
//                "x" to List(n) { rand.nextGaussian() }
//            )
            val numSeries = series.size
            val n = series[0].second.size

//            var data = mapOf(
//                "cycle" to (1..n).toList() + (1..n).toList(),
//                "y" to series1 + series2,
//                "Data" to List(n){"A"} + List(n){"B"}
//            }

//            val data = mutableMapOf<String, Any>()
//            data["cycle"] = (1..n).toList().repeat(numSeries)
//            data["Data"] = List(n) { index -> series[index / n].first }
//            data["y"] = series.flatMap { it.second }

            val data = mutableMapOf<String, Any>()
            data["cycle"] = (1..n).toList().flatMap { List(numSeries) { it } }
            data["Data"] = series.flatMap { (name, _) -> List(n) { name } }
            data["y"] = series.flatMap { (_, values) -> values }

////
//            val plot = letsPlot(data) +
//                    geomLine(color = "blue", showLegend = true) { x = "cycle"; y = "y1"} +
//                    geomLine(color = "red") { x = "cycle"; y = "y2" } +
//                    scaleColorContinuous("s1")
//
//            val rand = java.util.Random()
//            val data = mapOf(
//                "rating" to List(200) { rand.nextGaussian() } + List(200) { rand.nextGaussian() * 1.5 + 1.5 },
//                "cond" to List(200) { "A" } + List(200) { "B" }
//            )

//            val data = mapOf(
//                "supp" to listOf("OJ", "OJ", "OJ", "VC", "VC", "VC"),
//                "dose" to listOf(0.5, 1.0, 2.0, 0.5, 1.0, 2.0),
//                "length" to listOf(13.23, 22.70, 26.06, 7.98, 16.77, 26.14),
//                "len_min" to listOf(11.83, 21.2, 24.50, 4.24, 15.26, 23.35),
//                "len_max" to listOf(15.63, 24.9, 27.11, 10.72, 19.28, 28.93)
//            )

            val plot = letsPlot(data){x="cycle"; color="Data"} +
                    geomLine{y = "y"}

//            val plot = letsPlot(data) {x="dose"; color="supp"} +
//                    geomLine {y="length"}

//
//            var p = letsPlot(data)
//            p += geomDensity(color = "dark_green", alpha = .3) { x = "rating"; fill = "cond" }

            val rawSpec = plot.toSpec()
            val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

            val plotSwingPanel = DefaultPlotPanelJfx(
                processedSpec = processedSpec,
                preserveAspectRatio = false,
                preferredSizeFromPlot = false,
                repaintDelay = 10,
            ) { messages ->
                for (message in messages) {
                    println("[Example App] $message")
                }
            }

            Platform.runLater {
                val swingNode = SwingNode()
                swingNode.content = plotSwingPanel

                root.center = swingNode
            }


        }

        fun plotShow(plot: Plot){
            val rawSpec = plot.toSpec()
            val processedSpec = MonolithicCommon.processRawSpecs(rawSpec, frontendOnly = false)

            val plotSwingPanel = DefaultPlotPanelJfx(
                processedSpec = processedSpec,
                preserveAspectRatio = false,
                preferredSizeFromPlot = false,
                repaintDelay = 10,
            ) { messages ->
                for (message in messages) {
                    println("[Example App] $message")
                }
            }

            Platform.runLater {
                val swingNode = SwingNode()
                swingNode.content = plotSwingPanel

                root.center = swingNode
            }
        }

        fun startSimulation(params: Params, progressFun: (Double) -> Unit, returnFun: () -> Unit){
            data.clear()
            println(params.toString())
            thread {
                repeat(params.NC) { cycle ->
                    println("Cycle: $cycle")
                    // Client - provider pairs
                    val adj = Array(params.N) {IntArray(params.N)}
                    repeat(params.N){ i -> // providers
                        val clients = random.nextInt(params.KMin, params.KMax+1)
                        var inJ = 0
                        var rn = 0
                        while(inJ < clients){
                            rn = random.nextInt(0, params.N)
                            if(rn!=i && adj[i][rn]!=1){
                                adj[i][rn] = 1
                                inJ += 1
                            }
                        }
                    }
                    // Agents policy
                    var agentsR: MutableMap<Int, Double> = mutableMapOf()
                    repeat(params.N) {
                        val clients = adj[it]
                        val vProv = if (cycle == 0) params.V0 else data[cycle-1].V[it] // Vi
                        val numClients = clients.count{x->x == 1}
                        var meanPolicyR = .0
                        repeat(params.N){j->
                            if(adj[it][j]==1){
                                val vRepo = if (cycle == 0) params.V0 else data[cycle-1].V[j] // Vj
                                var L = hStep(vRepo, params.X) // L(Vj,x)
                                var p = if(params.sAgent!![it]==1) sBiasP(params.Y, L) else L // pij
                                if(params.sAgent!![it]==1 && params.sAgent!![j]==1) p = 1.0
                                val policyP = providerPolicy(randExpoD(params.ExpoA), p) // Pij
                                L = hStep(vProv, params.X) // L(Vi,x)
                                val r = if(params.sAgent!![j]==1) sBiasR(params.Z, L) else L // rij
                                val policyR = reporterPolicy(randExpoD(params.ExpoG), policyP, r) // Rij
                                meanPolicyR += policyR*vProv
                            }
                        }
                        meanPolicyR /= numClients // Provider reputation
                        agentsR[it] = meanPolicyR
                    }
                    // Reputation aggregation
                    // TODO Clustering
                    val sortedByR = agentsR.toList().sortedBy { it.second }.toMap() // order by R
//                    sortedByR.values.forEach {print("%.2f ".format(it))}
//                    println()
//                    sortedByR.keys.forEach {print("$it ")}
//                    println()
                    // two even sets
                    var meanRHigherSet = .0
                    var meanRLowerSet = .0
                    var iter = 0
                    sortedByR.values.forEach{
                        if(iter<sortedByR.size/2) {
                            meanRLowerSet += it
                        }
                        else{
                            meanRHigherSet += it
                        }
                        iter++
                    }
                    meanRHigherSet /= sortedByR.size/2
                    meanRLowerSet /= sortedByR.size/2
                    println("meanRHigherSet: ${String.format("%.2f",meanRHigherSet)}, meanRLowerSet: ${String.format("%.2f",meanRLowerSet)}")
                    // normalization (0,higher) -> (0,1)
                    meanRLowerSet /= meanRHigherSet
                    meanRHigherSet /= meanRHigherSet
                    // Trust measure
                    var V = DoubleArray(params.N)
                    iter = 0
                    sortedByR.keys.forEach{
                        if(iter<sortedByR.size/2) {
                            V[it] = meanRLowerSet
                        }
                        else{
                            V[it] = meanRHigherSet
                        }
                        iter++
                    }
                    var meanVs = 0.0
                    var meanVh = 0.0
                    repeat(params.N){
                        if(params.sAgent!![it]==1){
                            meanVs += V[it]
                        }
                        else{
                            meanVh += V[it]
                        }
                    }
                    meanVh /= params.N-params.S
                    meanVs /= params.S
                    val netOutflow = (meanVs + meanVh) / 2
                    data += Cycle(V, meanVs, meanVh, netOutflow, BooleanArray(params.N))
                    println("meanRHigherSet: ${String.format("%.2f",meanRHigherSet)}, meanRLowerSet: ${String.format("%.2f",meanRLowerSet)}")
                    println("meanVs: ${String.format("%.2f",meanVs)}, meanVh: ${String.format("%.2f",meanVh)}")
                    Thread.sleep(10)
                    progressFun((cycle+1).toDouble()/params.NC*100)
                }
                var series1: List<Double> = data.map {cycle->
                    cycle.meanVh
                }
                var series2: List<Double> = data.map {cycle->
                    cycle.meanVs
                }
                var series3: List<Double> = data.map {cycle->
                    cycle.netOutflow
                }

                val series = listOf(
                    Pair("meanVh", series1),
                    Pair("meanVs", series2),
                    Pair("netOutflow", series3)
                )

                plot(series)
                returnFun()
            }
        }
    }

}

fun main() {
    Locale.setDefault(Locale.US)
    Application.launch(AgentsApp::class.java)
}