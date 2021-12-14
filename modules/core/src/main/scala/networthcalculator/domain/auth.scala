package networthcalculator.domain

object auth {

  sealed trait Role {
    def roleRepr: String
  }

  object Role {

    final case class Admin() extends Role {
      override def roleRepr: String = "Admin"
    }

    final case class User() extends Role {
      def roleRepr: String = "User"
    }
  }

}
