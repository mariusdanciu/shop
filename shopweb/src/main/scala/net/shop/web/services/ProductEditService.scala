package net.shop.web.services

import net.shift.common.DefaultLog
import net.shift.common.Path
import net.shift.common.PathUtils
import net.shift.common.TraversingSpec
import net.shift.engine.http.POST
import net.shift.engine.http.Request
import net.shift.engine.utils.ShiftUtils
import net.shift.html.Formlet
import net.shift.html.Formlet.formToApp
import net.shift.html.Formlet.inputDouble
import net.shift.html.Formlet.inputFile
import net.shift.html.Formlet.inputInt
import net.shift.html.Formlet.inputSelect
import net.shift.html.Formlet.inputText
import net.shift.html.Formlet.listSemigroup
import net.shift.html.Validation
import net.shift.loc.Loc
import net.shift.template.Selectors
import net.shop.api.ProductDetail

object ProductEditService extends PathUtils with ShiftUtils with Selectors with TraversingSpec with DefaultLog with ProductValidation {

  def createProduct = for {
    r <- POST
    Path("product" :: "edit" :: Nil) <- path
  } yield {
  }

  private def extract(r: Request): Validation[ValidationError, ProductDetail] = {

    val params = r.params
    implicit val loc = r.language

    val product = ((ProductDetail.apply _).curried)(None)
    val ? = Loc.loc0(r.language) _

    val productFormlet = (Formlet(product) <*>
      inputText("edit_title")(validateMapField("edit_title", ?("title").text)) <*>
      inputText("edit_description")(validateMapField("edit_description", ?("description").text)) <*>
      inputText("edit_properties")(validateProps(?("edit_properties").text)) <*>
      inputDouble("edit_price")(validateDouble("edit_price", ?("price").text))) <*>
      inputText("cedit_oldPrice")(validateDefault("edit_oldPrice", None)) <*>
      inputInt("create_soldCount")(validateDefault("edit_soldCount", 0)) <*>
      inputSelect("edit_categories", Nil)(validateListField("edit_categories", ?("categories").text)) <*>
      inputFile("images")(validateDefault("images", Nil)) <*>
      inputSelect("edit_keywords", Nil)(validateListField("edit_keywords", ?("keywords").text))

    productFormlet validate params
  }
}