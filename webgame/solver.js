var myevent = document.createEvent('MouseEvents');
myevent.initEvent('click', false, true);
var tds = document.getElementsByClassName('card');

for (var i=0, il=tds.length; i<il; i++) {
	var card1 = tds[i];
	card1.dispatchEvent(myevent);
	card1.className = card1.className + " " + card1.style.background;
}

for (var i=0, il=tds.length-1; i<il; i++) {
	var card1 = tds[i];
	for (var j=i+1, jl=il+1; j<jl; j++) {
		var card2 = tds[j];
		if (card1.className.substring(6) === card2.className.substring(6)) {
			card1.dispatchEvent(myevent);
			card2.dispatchEvent(myevent);
			break;
		}
	}
}