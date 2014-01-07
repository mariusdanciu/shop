(function() {
  $(function() {

    $("#cart_symbol").click(function(event) {
      $('#cart_popup').show();
      event.stopPropagation();
    });
    
    $("#cart_popup").click(function(event) {
      event.stopPropagation();
    });

    $(document).click(function() {
      $('#cart_popup').hide();
    });
    
    $(document).keyup(function(e) {
     if (e.keyCode == 27) {
       $('#cart_popup').hide();
     }
    });
    
    $("#buy_step1").click(function(event) {
      cart.showStep1Links();
      event.stopPropagation();
    });
    
    $("#buy_step_cancel").click(function(event) {
      cart.showInitialLinks();
      event.stopPropagation();
    });
    
    cart = {

      computeTotal: function(){
        var total = 0;
        
        $(".cart_item").each(function() {
          var num = $(this).find("input").val();
          var price = $(this).find(".cart_price").text();
          total += num * price;
        });
        
        $("#total").text(parseFloat(total).toFixed(2) + " RON");
      },

      items : function(){
        var c = $.cookie("cart"); 
        if (c) {
           return $.parseJSON(c).items;
        }
        return [];
      },
      
      addItem : function(id) {
        var c = $.cookie("cart");
        if (c) {
           var cart = $.parseJSON(c);
           var a = cart.items;
           var found = undefined;
           for (i in a) {
             if (a[i].id === id) {
                found = a[i];
             }
           }
           if (!found) {
             cart.items.push({id: id, count: 1});
           } else {
             found.count = found.count + 1;
           }
           $.cookie("cart", JSON.stringify(cart));
        } else {
          $.cookie("cart", JSON.stringify({ items : [{id: id, count: 1}] }));
        }
        window.cart.loadView();
      },
   
      setItemCount : function(id, count) {
        var c = $.cookie("cart");
        if (c) {
          if (count === "") count = 1;
          
          var cart = $.parseJSON(c);
          var a = cart.items;
          for (i in a) {
            if (a[i].id === id) {
              a[i].count = parseInt(count);
            }
          }
          $.cookie("cart", JSON.stringify(cart));
          window.cart.computeTotal();
          $(this).focus();
        }
      },
   
      removeItem : function(id) {
        var c = $.cookie("cart");
        if (c) {
          var cart = $.parseJSON(c);
          var a = cart.items;
          var na = [];
          for (i in a) {
            if (a[i].id != id) {
              na.push(a[i])
            }
          }
          cart.items = na;
          $.cookie("cart", JSON.stringify(cart));
          window.cart.loadView();
        } 
      },
   
      loadView : function() {
        $.ajax({
		  url: "/getcart",
		  dataType: "json",
		  context: $("#cart_content")
        }).done(function(data) {
          $( this ).empty();
          var ul = document.createElement("ul");
          for (item in data) {
            var li = document.createElement("li");
            $(li).append(data[item]);
          	$(ul).append(li);
          }
          $( this ).append(ul);
          window.cart.computeTotal();
        }).fail(function(msg, f) {
            alert(f);
        });
      },
      
      showInitialLinks: function() {
        $('#order').hide();
        $('#buy_step1').show();
        $('#buy_final').hide();
        $('#buy_step_cancel').hide();
      },

      showStep1Links: function() {
        $('#order').show();
        $('#buy_step1').hide();
        $('#buy_final').show();
        $('#buy_step_cancel').show();
      }

   } 
   
   cart.showInitialLinks();
   cart.loadView();
  });
  
})();