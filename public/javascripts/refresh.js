temperature_chart = null;

function createChart(data, chartNumber, scaleLabel) {
  var config = {
	  type: 'line',
	  data: data,
	  options: {
	      responsive: true,
	      title:{
	          display:true,
	          text:'Temperature'
	      },
	      tooltips: {
	          mode: 'index',
	          intersect: false,
	      },
	      hover: {
	          mode: 'nearest',
	          intersect: true
	      },
	      scales: {
	          xAxes: [{
	              display: true,
	              scaleLabel: {
	                  display: true,
	                  labelString: 'Time'
	              }
	          }],
	          yAxes: [{
	              display: true,
	              scaleLabel: {
	                  display: true,
	                  labelString: 'Temperature'
	              }
	          }]
	      }
	  }		  
  };
  
  if (temperature_chart != null) {
	  temperature_chart.destroy();
  }
  
  var ctx = document.getElementById("chart"+chartNumber).getContext("2d");
  console.log("Creating chart");
  temperature_chart = new Chart(ctx, config);
}

Chart.defaults.global.animation = false;
Chart.defaults.global.scaleFontColor = "#AAA";
Chart.defaults.global.responsive = true;
Chart.defaults.global.tooltipTemplate = "<%if (label){%><%=label%>: <%}%><%= value %>";
Chart.defaults.global.scaleLabel = "<%=value%> C";
Chart.defaults.global.multiTooltipTemplate = "<%= datasetLabel %> - <%= value %>";
Chart.defaults.global.legendTemplate = "<ul class=\"<%=name.toLowerCase()%>-legend\"><% for (var i=0; i<datasets.length; i++){%><li><span style=\"background-color:<%=datasets[i].fillColor%>\"></span><%if(datasets[i].label){%><%=datasets[i].label%><%}%></li><%}%></ul>";
Chart.defaults.global.scaleGridLineColor = "rgba(255,255,255,.10)";

function loadData(timeframe) {
  $('#messages').html("");
  $.ajax({
    url: '/api/chartData?time=' + timeframe + '&type=temperature',
    success: function(data) {
      createChart(data, 1, "<%=value%> C");
    },
    error: function(jqXhr, textStatus, errorThrown) {
      $('#messages').html("<div class='" + textStatus + "'>" + textStatus + ": " + errorThrown + "</div>");
    }
  });
  /*
  $.ajax({
    url: '/api/chartData?time=' + timeframe + '&type=pressure',
    success: function(data) {
      createChart(data, 2, "<%=value%> kPa");
    },
    error: function(jqXhr, textStatus, errorThrown) {
      $('#messages').html("<div class='" + textStatus + "'>" + textStatus + ": " + errorThrown + "</div>");
    }
  });
  */
}


function onHashChange() {
  var hash = location.hash;
  var time = hash.substring(1);
  loadData(time);
  $("div.links a").removeClass("selected");
  $("div.links a." + time).addClass("selected");
}

$( document ).ready(function(){
  var hash = location.hash;
  if (hash == null || hash == "" || hash == "#") {
	location.hash = "#previous24h";
  } else {
	// The page load does not trigger a "change"
	onHashChange();
  }
});
