/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.server.jaxrs.providers.atom;



import org.jboss.resteasy.plugins.providers.jaxb.JAXBContextFinder;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBMarshalException;
import org.jboss.resteasy.plugins.providers.jaxb.JAXBUnmarshalException;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;

/**
 * TODO remove this file when JBoss AS includes RESTEasy 2.3.4.Final or higher
 */
@Provider
@Produces("application/atom+*")
@Consumes("application/atom+*")
public class AtomFeedProvider implements MessageBodyReader<Feed>, MessageBodyWriter<Feed>
{
   @Context
   protected Providers providers;

   protected JAXBContextFinder getFinder(MediaType type)
   {
      ContextResolver<JAXBContextFinder> resolver = providers.getContextResolver(JAXBContextFinder.class, type);
      if (resolver == null) return null;
      return resolver.getContext(null);
   }

   public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return Feed.class.isAssignableFrom(type);
   }

   public Feed readFrom(Class<Feed> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException
   {
      JAXBContextFinder finder = getFinder(mediaType);
      if (finder == null)
      {
         throw new JAXBUnmarshalException("Unable to find JAXBContext for media type: " + mediaType);
      }

      try
      {
         JAXBContext ctx = finder.findCachedContext(Feed.class, mediaType, annotations);
         Feed feed = (Feed) ctx.createUnmarshaller().unmarshal(entityStream);
         for (Entry entry : feed.getEntries())
         {
            if (entry.getContent() != null) entry.getContent().setFinder(finder);
         }
         return feed;
      }
      catch (JAXBException e)
      {
         throw new JAXBUnmarshalException("Unable to unmarshal: " + mediaType, e);
      }
   }

   public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return Feed.class.isAssignableFrom(type);
   }

   public long getSize(Feed feed, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
   {
      return -1;
   }

   public void writeTo(Feed feed, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException
   {
      JAXBContextFinder finder = getFinder(mediaType);
      if (finder == null)
      {
         throw new JAXBMarshalException("Unable to find JAXBContext for media type: " + mediaType);
      }
      HashSet<Class> set = new HashSet<Class>();
      set.add(Feed.class);
      for (Entry entry : feed.getEntries())
      {
         if (entry.getContent() != null && entry.getContent().getJAXBObject() != null)
         {
            set.add(entry.getContent().getJAXBObject().getClass());
         }
      }
      try
      {
         JAXBContext ctx = finder.findCacheContext(mediaType, annotations, set.toArray(new Class[set.size()]));
         Marshaller marshaller = ctx.createMarshaller();
        /* NamespacePrefixMapper mapper = new NamespacePrefixMapper()
         {
            public String getPreferredPrefix(String namespace, String s1, boolean b)
            {
               if (namespace.equals("http://www.w3.org/2005/Atom")) return "atom";
               else return s1;
            }
         };

         marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", mapper);
          */
         marshaller.marshal(feed, entityStream);
      }
      catch (JAXBException e)
      {
         throw new JAXBMarshalException("Unable to marshal: " + mediaType, e);
      }
   }
}
