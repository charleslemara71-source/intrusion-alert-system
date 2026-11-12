
var WHATSAPP_NUMBER = "254715425690";

function waLink(text) {
  return "https://wa.me/" + WHATSAPP_NUMBER + "?text=" + encodeURIComponent(text);
}

document.querySelectorAll("[data-wa]").forEach(function (el) {
  el.href = waLink(el.getAttribute("data-wa"));
  el.target = "_blank";
  el.rel = "noopener";
});

document.getElementById("qSend").addEventListener("click", function () {
  var name = document.getElementById("qName").value.trim();
  var type = document.getElementById("qType").value;
  var need = document.getElementById("qNeed").value.trim();
  if (!need) {
    document.getElementById("qNeed").focus();
    return;
  }
  var msg = "Hello TiRo Poultry, I would like a quote.\nName: " + (name || "-") +
            "\nI am a: " + type + "\nNeed: " + need;
  window.open(waLink(msg), "_blank", "noopener");
});

document.getElementById("yr").textContent = new Date().getFullYear();