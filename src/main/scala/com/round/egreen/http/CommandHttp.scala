// Copyright (c) 2018 Round, Inc.. All rights reserved.

package com.round.egreen.http

import cats.data.EitherT
import cats.effect.Effect
import cats.implicits._
import com.round.egreen.cqrs.command._
import com.round.egreen.service.{UserAuth, UserService}
import io.circe.{Decoder, Json}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl

class CommandHttp[F[_]: Effect](userAuth: UserAuth, userService: UserService[F]) extends Http4sDsl[F] {
  import CommandHttp._
  import UserAuth.UserClaim

  val service: HttpService[F] = userAuth {
    AuthedService[UserClaim, F] {
      case (request @ POST -> Root) as sender =>
        for {
          cmd <- request.as[CommandEnvelope]
          json <- (if (cmd.commandName == CreateUser.commandName) {
                     for {
                       userCmd <- parseCommand[CreateUser, F](cmd.json)
                                   .ensure(PERMISSION_DENIED) {
                                     _.username == "egreen" || CreateUser.permission.isAllowed(sender)
                                   }
                       json <- userService.createUser(userCmd)
                     } yield json
                   } else {
                     EitherT.leftT[F, Json](COMMAND_NOT_SUPPORTED)
                   }).value
          response <- json.fold(
                       e => BadRequest(s"{ error: $e }".asJson),
                       Ok(_)
                     )
        } yield response
    }
  }
}

object CommandHttp {
  import UserAuth.UserClaim

  val COMMAND_NOT_SUPPORTED = "command.not-supported"
  val COMMAND_NOT_PARSABLE  = "command.not-parsable"
  val PERMISSION_DENIED     = "permission.denied"

  def ensureCommand[C, F[_]](json: String, sender: UserClaim)(implicit
                                                              D: Decoder[C],
                                                              P: Permission[C],
                                                              F: Effect[F]): EitherT[F, String, C] =
    parseCommand[C, F](json)
      .ensure(PERMISSION_DENIED)(_ => P.isAllowed(sender))

  def parseCommand[C: Decoder, F[_]: Effect](json: String): EitherT[F, String, C] =
    EitherT.fromEither[F](
      parse(json).flatMap(_.as[C]).leftMap(_ => COMMAND_NOT_PARSABLE)
    )
}
