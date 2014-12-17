(function() {
  $(function() {
    $('#create_product_tab').tabify();

    $("#create_product").click(function(event) {
      window.admin.saveProduct("#upload_form", function() {
        window.products.closeDialog();
        window.products.reloadProducts();
      });
      event.stopPropagation();
      event.preventDefault();
    });

    admin.addProp("#add_prop", "#prop_fields");
    admin.toggleDescription("create");

  });

})();

var admin = {
  addProp : function(elem, holder) {
    $(elem).click(function(event) {
      var div = $("<div class='row'></div>");
      div.append("<input type='text' name='pkey'/><input type='text' name='pval'/>");

      var remove = $("<img class='clickable' src='/static/images/minus.png'/>");
      remove.click(function(e) {
        div.remove();
      });

      div.append(remove);
      $(holder).append(div);

    });
  },

  deleteProduct : function(id) {
    $.ajax({
      url : "/product/delete/" + id,
      type : "DELETE"
    }).success(products.reloadProducts()).fail(function(msg, f) {
      $("#notice_connect_e").show().delay(5000).fadeOut("slow");
    });
  },

  saveProduct : function(formId, successFunc) {
    $(formId).each(function() {
      var frm = this;
      var formData = new FormData(frm);

      $(formId + ' label').css("color", "#000000").removeAttr("title");
      $.ajax({
        url : $(frm).attr('action'),
        type : "POST",
        cache : false,
        contentType : false,
        processData : false,
        data : formData,
        statusCode : {
          201 : function() {
            successFunc();
          }
        }
      }).fail(function(msg, f) {
        $("#notice_connect_e").show().delay(5000).fadeOut("slow");
      })
    });
  },

  toggleDescription : function(prefix) {
    $("#" + prefix + "_toggle_desc").click(function(e) {
      $("." + prefix + "_toggle_text").toggle();
      $("." + prefix + "_toggle").toggle({
        duration : 0,
        done : function() {
          var preview = $("#" + prefix + "_description_view");
          if (preview.css('display') != 'none') {
            preview.html(textile.convert($("#" + prefix + "_description").val()));
          }
        }
      });
    });
    
  },
  
  attachToProduct : function(successFunc) {
    $("#save_product").click(function(event) {
      admin.saveProduct("#edit_form", successFunc);
      event.stopPropagation();
      event.preventDefault();
    });
    $('#edit_product_tab').tabify();
    $("#edit_specs .row img").click(function(e) {
      var row = $(this).parent();
      row.remove();
    });
    admin.addProp("#edit_add_prop", "#edit_prop_fields");
    admin.toggleDescription("edit");
  },

  createProduct : function() {
    $("#itemadd").click(function(event) {
      $.blockUI({
        message : $("#product_create_dialog"),
        css : {
          top : '70px',
          left : ($(window).width() - 800) / 2 + 'px',
          width : '800px',
          border : 'none',
          cursor : null
        },
        overlayCSS : {
          cursor : null,
          backgroundColor : '#dddddd'
        }
      });
    });
  }

};