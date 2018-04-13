
$.fn.extend({
		//---元素拖动插件
    dragging:function(data){  
		var $this = $(this);
		//1

    	$("#small_button").click( function () {
				var w=$(window).width()-$(".box-3 dl").width();
				var h=$(window).height()-$(".box-3 dl dd").height();
				var h1=$(window).height()-475;				
			if ( $(this).hasClass("up") ){
					$(this).removeClass("up").addClass("down");
					$(".box-3 dl").find("ul").show();
					//$(".box-3 dl").animate({
					//	left:w+"px",
					//	top:h+"px"
					//},200);
					$("#botname").text("● PTV Bot");								
				}else{	
					$(this).removeClass("down").addClass("up");
					$(".box-3 dl").find("ul").hide();	
					//$(".box-3 dl").animate({
					//	left:(w-80)+"px",
					//	top:h1+"px"
					//},200);
					$("#botname").text("● Need help? Click me!");					
				}
		
		 });	
		 $("#small_button2").click( function () {
			$("#small_button").click();
		 });			
    }
	
}); 