package eventer

import zio.Has

package object application {
  type Jwts = Has[Jwts.Service]
}
