package com.example.agentsjfx

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Slider

class MainController {
    @FXML
    private lateinit var sliderN: Slider
    @FXML
    private lateinit var sliderS: Slider
    @FXML
    private lateinit var sliderNC: Slider
    @FXML
    private lateinit var sliderKMin: Slider
    @FXML
    private lateinit var sliderKMax: Slider
    @FXML
    private lateinit var sliderExpoA: Slider
    @FXML
    private lateinit var sliderExpoG: Slider
    @FXML
    private lateinit var sliderV0: Slider
    @FXML
    private lateinit var sliderX: Slider
    @FXML
    private lateinit var sliderY: Slider
    @FXML
    private lateinit var sliderZ: Slider
    @FXML
    private lateinit var buttonSS: Button
    @FXML
    private lateinit var ln: Label
    @FXML
    private lateinit var ls: Label
    @FXML
    private lateinit var lnc: Label
    @FXML
    private lateinit var lkmin: Label
    @FXML
    private lateinit var lkmax: Label
    @FXML
    private lateinit var lea: Label
    @FXML
    private lateinit var leg: Label
    @FXML
    private lateinit var lv0: Label
    @FXML
    private lateinit var lx: Label
    @FXML
    private lateinit var ly: Label
    @FXML
    private lateinit var lz: Label

    fun initialize(){
        ln.text = "Num agents N = ${sliderN.value}"
        ls.text = "Num strategic S = ${sliderS.value}"
        lnc.text = "Num cycles = ${sliderNC.value}"
        lkmax.text = "Service providers max k_max = ${sliderKMax.value}"
        lkmin.text = "Service providers min k_min = ${sliderKMin.value}"
        lv0.text = "Initial trust measure V_0 = ${sliderV0.value}"
        lea.text = "Service measure expoA = ${sliderExpoA.value}"
        leg.text = "Service measure expoG = ${sliderExpoG.value}"
        lx.text = "Honest policy x = ${sliderX.value}"
        ly.text = "Strategic policy y = ${sliderY.value}"
        lz.text = "Strategic policy z = ${sliderZ.value}"
        buttonSS.setOnAction {
            val params = AgentsApp.Companion.Params(
                sliderN.value.toInt(), sliderS.value.toInt(), sliderNC.value.toInt(),
                sliderKMin.value.toInt(),sliderKMax.value.toInt(),
                sliderExpoA.value,sliderExpoG.value,sliderV0.value,sliderX.value,sliderY.value,sliderZ.value, null)
            AgentsApp.startSimulation(params, { progress ->
                println("${"%.2f".format(progress)}")
            })
            {
                println("completed")
            }
        }
        sliderN.valueProperty().addListener { _, oldValue, newValue ->
            ln.text = "Num agents N = $newValue"
        }
        sliderS.valueProperty().addListener { _, oldValue, newValue ->
            ls.text = "Num strategic S = $newValue"
        }
        sliderNC.valueProperty().addListener { _, oldValue, newValue ->
            lnc.text = "Num cycles = $newValue"
        }
        sliderKMax.valueProperty().addListener { _, oldValue, newValue ->
            lkmax.text = "Service providers max k_max = $newValue"
        }
        sliderKMin.valueProperty().addListener { _, oldValue, newValue ->
            lkmin.text = "Service providers min k_min = $newValue"
        }
        sliderV0.valueProperty().addListener { _, oldValue, newValue ->
            lv0.text = "Initial trust measure V_0 = $newValue"
        }
        sliderExpoA.valueProperty().addListener { _, oldValue, newValue ->
            lea.text = "Service measure expoA = $newValue"
        }
        sliderExpoG.valueProperty().addListener { _, oldValue, newValue ->
            leg.text = "Service measure expoG = $newValue"
        }
        sliderX.valueProperty().addListener { _, oldValue, newValue ->
            lx.text = "Honest policy x = $newValue"
        }
        sliderY.valueProperty().addListener { _, oldValue, newValue ->
            ly.text = "Strategic policy y = $newValue"
        }
        sliderZ.valueProperty().addListener { _, oldValue, newValue ->
            lz.text = "Strategic policy z = $newValue"
        }
    }

}