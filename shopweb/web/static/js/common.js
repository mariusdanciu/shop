(function() {
  $(function() {

    $("#menu").tabify();
    $("#cart_symbol").click(function(event) {
      window.cart.showCart();
      event.stopPropagation();
      event.preventDefault();
    });

    $("#user_symbol").click(function(event) {
      window.user.showLogin();
      event.stopPropagation();
      event.preventDefault();
    });

    $("#cart_popup, #user_popup, #newuser_popup").click(function(event) {
      event.stopPropagation();
    });

    $("#authgo").click(function(event) {
      window.user.login("#login_form");
      event.stopPropagation();
      event.preventDefault();
    });

    $("#newuser").click(function(event) {
      window.user.hideLogin();
      window.user.showNewUser();
      event.stopPropagation();
      event.preventDefault();
    });

    $("#save_user").click(function(event) {
      window.user.createUser("#newuser_form");
      event.stopPropagation();
      event.preventDefault();
    });
    
    $('#forgotpass').click(function(event) {
      window.user.forgotPass("#login_form");
      event.stopPropagation();
      event.preventDefault();
    });

    $("#login_form").keydown(function(event) {
      if (event.keyCode == 13) {
        window.user.login("#login_form");
        event.stopPropagation();
        event.preventDefault();
        return false;
      }
    });

    $("#logout").click(function(event) {
      window.user.logout();
      event.stopPropagation();
      event.preventDefault();
    });

    $(".close_dialog").bind("click", function(event) {
      common.closeDialog();
    });

    $(document).click(function() {
      cart.hideCart();
      user.hideLogin();
    });

    $(document).keyup(function(e) {
      if (e.keyCode == 27) {
        common.closeDialog();
        cart.hideCart();
        user.hideLogin();
      }
    });

    $("#buy_step1").click(function(event) {
      cart.fetchUserInfo();
      cart.showStep1Links();
      event.stopPropagation();
      event.preventDefault();
    });

    $('#buy_final, #c_buy_final_comp').click(function(event) {
      var clicked = $(this).attr("id");
      var form = (clicked !== "c_buy_final_comp") ? "#order_form" : "#order_form_company";

      var obj = $(form).serializeArray();
      var items = cart.items();
      for (e in items) {
        obj.push({
          name : "item." + items[e].id,
          value : items[e].count
        })
      }

      cart.cleanFormMessages();
      $.ajax({
        url : "/order",
        data : obj
      }).fail(function(msg, f) {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      });
      event.preventDefault();
    });

    $("#search_text").keypress(function(e) {
      if (e.which == 13) {
        var text = $("#search_text").val();
        window.location.href = '/products?search=' + text;
      }
    });

    window.cart.loadView();
  });

})();

var common = {
  closeDialog : function() {
    $.unblockUI();
    $("#user_popup").hide();
  },
  
  showNotice : function(text) {
    $("#notice_i").html(text);
    $("#notice_i").show().delay(5000).fadeOut("slow");
  },
  
  showError : function(text) {
    $("#notice_e").html(text);
    $("#notice_e").show().delay(5000).fadeOut("slow");
  }

}

var user = {
  showNewUser : function() {
    $.blockUI({
      message : $("#newuser_popup"),
      css : {
        top : '150px',
        left : ($(window).width() - 630) / 2 + 'px',
        width : '630px',
        border : 'none',
        cursor : null
      },
      overlayCSS : {
        cursor : null,
        backgroundColor : '#dddddd'
      }
    });
  },

  showLogin : function() {
    $("#user_popup").show();
  },

  hideLogin : function() {
    $("#user_popup").hide();
  },

  logout : function() {
    window.location.href = "/?logout=true";
  },
  
  forgotPass : function(frmId) {
    var email = $.base64.encode($(frmId + " #username").val());
    $.ajax({
      url : "/forgotpassword/" + email,
      type : "POST",
      cache : false,
    }).success(function(){
      common.closeDialog();
    }).fail(function(msg, f) {
      $("#notice_connect_e").html(msg.responseText);
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  },

  login : function(frmId) {
    var creds = $.base64.encode($(frmId + " #username").val() + ":" + $(frmId + " #password").val());

    $.ajax({
      url : $(frmId).attr('action'),
      type : "GET",
      cache : false,
      headers : {
        'Authorization' : "Basic " + creds
      },
      statusCode : {
        200 : function() {
          window.location.href = "/";
        }
      }
    }).fail(function(msg, f) {
      $("#notice_connect_e").html(msg.responseText);
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  },

  createUser : function(formId) {
    $(formId).each(function() {
      var frm = this;

      $(formId + ' label').css("color", "#555555").removeAttr("title");
      $.ajax({
        url : $(formId).attr('action'),
        type : "POST",
        cache : false,
        data :  $(formId).serialize(),
        statusCode : {
          201 : function() {
            common.closeDialog();
          }
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").html(msg.responseText);
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      });
    });
  },

}

var cart = {

  fetchUserInfo : function() {
    $.ajax({
      url : "/userinfo",
      dataType : "json",
      context : $("#cart_content")
    }).done(function(data) {
      cart.populateForm(data);
    });
  },  
  
  populateForm : function(data){
    $("#order_form #fname").attr("value", data.userInfo.firstName);
    $("#order_form #lname").attr("value", data.userInfo.lastName);
    $("#order_form #email").attr("value", data.email);
    $("#order_form #phone").attr("value", data.userInfo.phone);
    $("#order_form #cnp").attr("value", data.userInfo.cnp);
    
    $("#order_form_company #cname").attr("value", data.companyInfo.name);
    $("#order_form_company #cif").attr("value", data.companyInfo.cif);
    $("#order_form_company #cregcom").attr("value", data.companyInfo.regCom);
    $("#order_form_company #cbank").attr("value", data.companyInfo.bank);
    $("#order_form_company #cbankaccount").attr("value", data.companyInfo.bankAccount);
    $("#order_form_company #cemail").attr("value", data.email);
    $("#order_form_company #cphone").attr("value", data.companyInfo.phone);
  },
    
  cleanFormMessages : function() {
    $('#order_form label, #order_form_company label').css("color", "#555555").removeAttr("title");
  },

  clear : function() {
    $.cookie("cart", JSON.stringify({
      items : []
    }));
    window.cart.loadView();
  },

  computeTotal : function() {
    var total = 0;

    $(".cart_item").each(function() {
      var num = $(this).find("input").val();
      var price = $(this).find(".cart_price").text();
      total += num * price;
    });

    $("#total").text(parseFloat(total).toFixed(2) + " RON");
  },

  items : function() {
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
        cart.items.push({
          id : id,
          count : 1
        });
      } else {
        found.count = found.count + 1;
      }
      $.cookie("cart", JSON.stringify(cart));
    } else {
      $.cookie("cart", JSON.stringify({
        items : [ {
          id : id,
          count : 1
        } ]
      }));
    }
  },

  setItemCount : function(id, count) {
    var c = $.cookie("cart");
    if (c) {
      if (count === "")
        count = 1;

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

  loadView : function(f) {
    if (window.cart.items().length === 0) {
      $('#order').hide();
      $('#cart_content').hide();
      $('#cart_footer').hide();
      $('#cart_empty').show();
      if (f !== undefined) {
        f();
      }
    } else {
      $('#cart_empty').hide();

      $.ajax({
        url : "/getcart",
        dataType : "json",
        context : $("#cart_content")
      }).done(function(data) {
        $(this).empty();
        var ul = document.createElement("ul");
        for (item in data) {
          var li = document.createElement("li");
          $(li).append(data[item]);
          $(ul).append(li);
        }
        $(this).append(ul);
        window.cart.computeTotal();

        $('#cart_content').show();
        $('#cart_footer').show();

        $(".cart_item a").each(function(index) {
          var me = $(this);
          var id = me.attr("id").substring(4);
          
          $('#q_' + id).on("keyup change", function(e) {
            window.cart.setItemCount(id, $(this).val());
            e.preventDefault();
          });
          me.click(function(e) {
            window.cart.removeItem(id);
            e.preventDefault();
          });
        });

        if (f !== undefined) {
          f();
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      });
    }
  },

  showStep1Links : function() {
    $('#order').show();
    $('#buy_step1').hide();
  },

  showCart : function() {
    window.cart.loadView(function() {
      $('#order').hide();
      $('#cart_notice').hide();
      $('#buy_step1').show();
      $('#cart_popup').show();
    });

  },

  hideCart : function() {
    $('#cart_popup').hide();
    $('#cart_notice').hide();
    $('#order').hide();
    $('#buy_step1').show();
  }

}
