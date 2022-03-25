Blockly.Blocks['playcanvas_initialize'] = {
  init: function() {
    this.appendValueInput("script name")
        .setCheck("String")
        .appendField("");
    this.setColour(315);
 this.setTooltip("initilize playcanvas script");
 this.setHelpUrl("");
  }
};